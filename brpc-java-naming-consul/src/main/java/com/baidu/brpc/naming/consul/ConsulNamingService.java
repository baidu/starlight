package com.baidu.brpc.naming.consul;

import com.baidu.brpc.client.endpoint.EndPoint;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.naming.*;
import com.baidu.brpc.naming.consul.model.ConsulConstants;
import com.baidu.brpc.naming.consul.model.ConsulResponse;
import com.baidu.brpc.naming.consul.model.ConsulService;
import com.baidu.brpc.utils.CustomThreadFactory;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.health.model.HealthService;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import io.netty.util.internal.ConcurrentSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.concurrent.*;

@Slf4j
public class ConsulNamingService implements NamingService {

    private BrpcURL      url;
    private ConsulClient client;
    private int          retryInterval;
    private int          consulInterval;
    private int          lookupInterval;
    private ConcurrentSet<RegisterInfo> failedRegisters   =
            new ConcurrentSet<RegisterInfo>();
    private ConcurrentSet<RegisterInfo> failedUnregisters =
            new ConcurrentSet<RegisterInfo>();

    private ConcurrentMap<SubscribeInfo, NotifyListener> failedSubscribes   =
            new ConcurrentHashMap<SubscribeInfo, NotifyListener>();
    private ConcurrentSet<SubscribeInfo>                 failedUnsubscribes =
            new ConcurrentSet<SubscribeInfo>();

    private Timer timer;

    private final ConcurrentMap<String, Long> lookupGroupServices = new ConcurrentHashMap<String, Long>();

    private Set<String> serviceIds = new ConcurrentSet<String>();
    private ScheduledExecutorService heartbeatExecutor;

    private ConcurrentHashMap<String, Future> consulLookupFuture = new ConcurrentHashMap<String, Future>();

    private ConcurrentHashMap<String, ConcurrentHashMap<String, List<EndPoint>>> serviceCache = new ConcurrentHashMap<String, ConcurrentHashMap<String, List<EndPoint>>>();

    public ConsulNamingService(BrpcURL url) {
        this.url = url;
        try {
            String[] hostPorts = url.getHostPorts().split(":");
            this.client = new ConsulClient(hostPorts[0], Integer.parseInt(hostPorts[1]));
        } catch (Exception e) {
            throw new RpcException(RpcException.SERVICE_EXCEPTION,
                    "wrong configuration of url, should be like test.bj:port");
        }

        this.retryInterval = url.getIntParameter(Constants.INTERVAL, Constants.DEFAULT_INTERVAL);
        this.consulInterval = url.getIntParameter(ConsulConstants.CONSULINTERVAL, ConsulConstants.DEFAULT_CONSUL_INTERVAL);
        this.lookupInterval = url.getIntParameter(ConsulConstants.LOOKUPINTERVAL, ConsulConstants.DEFAULT_LOOKUP_INTERVAL);
        timer = new HashedWheelTimer(new CustomThreadFactory("consul-retry-timer-thread"));

        timer.newTimeout(
                new TimerTask() {
                    @Override
                    public void run(Timeout timeout) throws Exception {
                        try {
                            for (RegisterInfo registerInfo : failedRegisters) {
                                register(registerInfo);
                            }
                            for (RegisterInfo registerInfo : failedUnregisters) {
                                unregister(registerInfo);
                            }
                            for (Map.Entry<SubscribeInfo, NotifyListener> entry : failedSubscribes.entrySet()) {
                                subscribe(entry.getKey(), entry.getValue());
                            }
                            for (SubscribeInfo subscribeInfo : failedUnsubscribes) {
                                unsubscribe(subscribeInfo);
                            }
                        } catch (Exception ex) {
                            log.warn("retry timer exception:", ex);
                        }
                        timer.newTimeout(this, retryInterval, TimeUnit.MILLISECONDS);
                    }
                },
                retryInterval, TimeUnit.MILLISECONDS);

        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();

        heartbeatExecutor.scheduleAtFixedRate(new Runnable() {
                                                  @Override
                                                  public void run() {
                                                      for (String tempService : serviceIds) {
                                                          client.agentCheckPass(tempService);
                                                          log.debug("Sending consul heartbeat for: {}", tempService);
                                                      }
                                                  }
                                              }, ConsulConstants.HEARTBEAT_CIRCLE,
                ConsulConstants.HEARTBEAT_CIRCLE, TimeUnit.MILLISECONDS);
    }

    public void destroy() {
        serviceIds.clear();
        timer.stop();
        heartbeatExecutor.shutdown();
    }

    @Override public List<EndPoint> lookup(SubscribeInfo subscribeInfo) {

        List<EndPoint> endPoints = new ArrayList<EndPoint>();

        try {

            ConcurrentHashMap<String, List<EndPoint>> serviceUpdate = lookupServiceUpdate(subscribeInfo.getGroup());

            if (!serviceUpdate.isEmpty() && serviceUpdate.containsKey(subscribeInfo.getService())) {
                endPoints = serviceUpdate.get(subscribeInfo.getService());
            }
        } catch (Exception ex) {
            log.warn("lookup end point list failed from {}, msg={}",
                    url, ex.getMessage());
            if (!subscribeInfo.isIgnoreFailOfNamingService()) {
                throw new RpcException("lookup end point list failed from consul failed", ex);
            }
        }

        return endPoints;
    }

    @Override public void subscribe(final SubscribeInfo subscribeInfo, final NotifyListener listener) {

        final String path = getSubscribePath(subscribeInfo);

        Future future = heartbeatExecutor.scheduleAtFixedRate(new Runnable() {
                                                  @Override
                                                  public void run() {
                                                      try {

                                                          long time = System.currentTimeMillis();

                                                          log.debug("heart beat schedule, time: {}", time);
                                                          Map<String, List<EndPoint>> endpointForGroup = serviceCache
                                                                  .get(subscribeInfo.getGroup());
                                                          List<EndPoint> currentEndPoints = lookup(subscribeInfo);

                                                          ConcurrentHashMap<String, List<EndPoint>> serviceUpdate = lookupServiceUpdate(
                                                                  subscribeInfo.getGroup());

                                                          updateServiceCache(subscribeInfo.getGroup(), serviceUpdate);

                                                          log.debug("heart beat schedule, lookup and update time: {}", System.currentTimeMillis() - time);

                                                          if ((endpointForGroup != null && !endpointForGroup.isEmpty()) ||
                                                                  !currentEndPoints.isEmpty()) {
                                                              List<EndPoint> lastEndPoints = new ArrayList<EndPoint>();
                                                              if (endpointForGroup != null) {
                                                                  lastEndPoints = endpointForGroup.get(subscribeInfo.getService());
                                                              }

                                                              Collection<EndPoint> addList = CollectionUtils.subtract(
                                                                      currentEndPoints, lastEndPoints);
                                                              Collection<EndPoint> deleteList = CollectionUtils.subtract(
                                                                      lastEndPoints, currentEndPoints);
                                                              listener.notify(addList, deleteList);

                                                              failedSubscribes.remove(subscribeInfo);
                                                          }
                                                          log.info("subscribe success from {}", url);
                                                      } catch (Exception e) {
                                                          if (!subscribeInfo.isIgnoreFailOfNamingService()) {
                                                              throw new RpcException("subscribe failed from " + url, e);
                                                          } else {
                                                              failedSubscribes.putIfAbsent(subscribeInfo, listener);
                                                          }
                                                      }
                                                  }

                                              }, ConsulConstants.HEARTBEAT_CIRCLE,
                ConsulConstants.DEFAULT_LOOKUP_INTERVAL, TimeUnit.MILLISECONDS);

        consulLookupFuture.putIfAbsent(path, future);
    }

    @Override public void unsubscribe(SubscribeInfo subscribeInfo) {
        try {

            String path = getSubscribePath(subscribeInfo);
            consulLookupFuture.get(path).cancel(false);

            log.info("unsubscribe success from {}", url);
        } catch (Exception ex) {
            if (!subscribeInfo.isIgnoreFailOfNamingService()) {
                throw new RpcException("unsubscribe failed from " + url, ex);
            } else {
                failedUnsubscribes.add(subscribeInfo);
                return;
            }
        }
    }

    public String getSubscribePath(SubscribeInfo subscribeInfo) {
        return subscribeInfo.getGroup() + "-" + subscribeInfo.getService() + "-" + subscribeInfo.getVersion();
    }

    @Override public void register(RegisterInfo registerInfo) {
        try {

            NewService newService = getConsulNewService(registerInfo);
            client.agentServiceRegister(newService);

            serviceIds.add("service:" + newService.getId());

            log.info("register success to {}", url);
        } catch (Exception ex) {
            if (!registerInfo.isIgnoreFailOfNamingService()) {
                throw new RpcException("Failed to register to " + url, ex);
            } else {
                failedRegisters.add(registerInfo);
                return;
            }
        }
        failedRegisters.remove(registerInfo);
    }

    @Override public void unregister(RegisterInfo registerInfo) {
        try {
            NewService newService = getConsulNewService(registerInfo);
            client.agentServiceDeregister(newService.getId());

            serviceIds.remove("service:" + newService.getId());
        } catch (Exception ex) {
            if (!registerInfo.isIgnoreFailOfNamingService()) {
                throw new RpcException("Failed to unregister to " + url, ex);
            } else {
                failedUnregisters.add(registerInfo);
                return;
            }
        }
        failedUnregisters.remove(registerInfo);
    }

    private NewService getConsulNewService(RegisterInfo registerInfo) {
        NewService newService = new NewService();
        newService.setName(registerInfo.getGroup());
        newService.setId(getRegisterPath(registerInfo));
        newService.setAddress(registerInfo.getHost());
        newService.setPort(registerInfo.getPort());
        newService.setTags(Arrays.asList(registerInfo.getGroup(), registerInfo.getService()));
        NewService.Check check = new NewService.Check();
        check.setTtl(this.consulInterval + "s");
        check.setDeregisterCriticalServiceAfter("3m");

        newService.setCheck(check);

        return newService;
    }

    public String getRegisterPath(RegisterInfo registerInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append(registerInfo.getHost()).append(":").append(registerInfo.getPort()).append("-")
                .append(registerInfo.getService());
        return sb.toString();

    }

    private void updateServiceCache(String group, ConcurrentHashMap<String, List<EndPoint>> groupUrls) {
        if (groupUrls != null && !groupUrls.isEmpty()) {
            ConcurrentHashMap<String, List<EndPoint>> groupMap = serviceCache.get(group);
            if (groupMap == null) {
                serviceCache.put(group, groupUrls);
            }
            for (Map.Entry<String, List<EndPoint>> entry : groupUrls.entrySet()) {
                if (groupMap != null) {
                    groupMap.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    private ConcurrentHashMap<String, List<EndPoint>> lookupServiceUpdate(String group) {
        ConcurrentHashMap<String, List<EndPoint>> groupUrls = new ConcurrentHashMap<String, List<EndPoint>>();
        Long lastConsulIndexId = lookupGroupServices.get(group) == null ? 0 : lookupGroupServices.get(group);

        long time = System.currentTimeMillis();

        ConsulResponse<List<ConsulService>> response = lookupConsulService(group, lastConsulIndexId);
        log.debug("lookupConsulService, time: {}", System.currentTimeMillis() - time);

        if (response != null) {
            List<ConsulService> services = response.getValue();
            if (services != null && !services.isEmpty()
                    && response.getConsulIndex() > lastConsulIndexId) {
                for (ConsulService service : services) {
                    try {
                        String serviceName = "";
                        List<String> tags = service.getTags();
                        for (String tag : tags) {
                            if (!tag.equals(service.getName())) {
                                serviceName = tag;
                            }
                        }
                        List<EndPoint> urlList = groupUrls.get(serviceName);

                        if (urlList == null) {
                            urlList = new ArrayList<EndPoint>();
                        }
                        urlList.add(new EndPoint(service.getAddress(), service.getPort()));
                        groupUrls.put(serviceName, urlList);
                    } catch (Exception e) {
                    }

                }
                lookupGroupServices.put(group, response.getConsulIndex());
                return groupUrls;
            } else {
            }
        }
        return groupUrls;
    }

    private ConsulResponse<List<ConsulService>> lookupConsulService(String serviceName, Long lastConsulIndexId) {
        ConsulResponse<List<ConsulService>> response = lookupHealthService(
                serviceName,
                lastConsulIndexId);
        return response;
    }

    public ConsulResponse<List<ConsulService>> lookupHealthService(
            String serviceName, long lastConsulIndex) {
        QueryParams queryParams = new QueryParams(
                ConsulConstants.CONSUL_BLOCK_TIME_SECONDS, lastConsulIndex);
        Response<List<HealthService>> orgResponse = client.getHealthServices(
                serviceName, true, queryParams);
        ConsulResponse<List<ConsulService>> newResponse = null;
        if (orgResponse != null && orgResponse.getValue() != null
                && !orgResponse.getValue().isEmpty()) {
            List<HealthService> HealthServices = orgResponse.getValue();
            List<ConsulService> ConsulServices = new ArrayList<ConsulService>(
                    HealthServices.size());

            for (HealthService orgService : HealthServices) {
                try {
                    ConsulService newService = convertToConsulService(orgService);
                    ConsulServices.add(newService);
                } catch (Exception e) {
                    String servcieid = "null";
                    if (orgService.getService() != null) {
                        servcieid = orgService.getService().getId();
                    }
                    log.info("Consul lookup health service error, service id:{}", servcieid);
                }
            }
            if (!ConsulServices.isEmpty()) {
                newResponse = new ConsulResponse<List<ConsulService>>();
                newResponse.setValue(ConsulServices);
                newResponse.setConsulIndex(orgResponse.getConsulIndex());
                newResponse.setConsulLastContact(orgResponse
                        .getConsulLastContact());
                newResponse.setConsulKnownLeader(orgResponse
                        .isConsulKnownLeader());
            }
        }

        return newResponse;
    }

    private ConsulService convertToConsulService(HealthService healthService) {
        ConsulService service = new ConsulService();
        HealthService.Service org = healthService.getService();
        service.setAddress(org.getAddress());
        service.setId(org.getId());
        service.setName(org.getService());
        service.setPort(org.getPort());
        service.setTags(org.getTags());
        return service;
    }

}