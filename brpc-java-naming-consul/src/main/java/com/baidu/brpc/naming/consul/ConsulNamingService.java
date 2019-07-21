/*
 * Copyright (c) 2018 Baidu, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baidu.brpc.naming.consul;

import com.baidu.brpc.client.instance.ServiceInstance;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;

@Slf4j
public class ConsulNamingService implements NamingService {

    private BrpcURL url;
    private ConsulClient client;
    private int retryInterval;
    private int consulInterval;
    private int lookupInterval;
    private ConcurrentSet<RegisterInfo> failedRegisters   =
            new ConcurrentSet<RegisterInfo>();
    private ConcurrentSet<RegisterInfo> failedUnregisters =
            new ConcurrentSet<RegisterInfo>();

    private ConcurrentMap<SubscribeInfo, NotifyListener> failedSubscribes   =
            new ConcurrentHashMap<SubscribeInfo, NotifyListener>();
    private ConcurrentSet<SubscribeInfo> failedUnsubscribes =
            new ConcurrentSet<SubscribeInfo>();

    private Timer timer;

    private final ConcurrentMap<String, Long> lookupGroupServices = new ConcurrentHashMap<String, Long>();

    private Set<String> serviceIds = new ConcurrentSet<String>();
    private ScheduledExecutorService heartbeatExecutor;

    private ConcurrentHashMap<String, Future> consulLookupFuture = new ConcurrentHashMap<String, Future>();

    private ConcurrentHashMap<String, ConcurrentHashMap<String, List<ServiceInstance>>> serviceCache
            = new ConcurrentHashMap<String, ConcurrentHashMap<String, List<ServiceInstance>>>();

    public ConsulNamingService(BrpcURL url) {
        this.url = url;
        try {
            String[] hostPorts = url.getHostPorts().split(":");
            this.client = new ConsulClient(hostPorts[0], Integer.parseInt(hostPorts[1]));
        } catch (Exception e) {
            throw new RpcException(RpcException.SERVICE_EXCEPTION,
                    "wrong configuration of url, should be like test.bj:port", e);
        }

        this.retryInterval = url.getIntParameter(Constants.INTERVAL, Constants.DEFAULT_INTERVAL);
        this.consulInterval = url.getIntParameter(ConsulConstants.CONSULINTERVAL,
                ConsulConstants.DEFAULT_CONSUL_INTERVAL);
        this.lookupInterval = url.getIntParameter(ConsulConstants.LOOKUPINTERVAL,
                ConsulConstants.DEFAULT_LOOKUP_INTERVAL);
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

    @Override
    public List<ServiceInstance> lookup(SubscribeInfo subscribeInfo) {

        List<ServiceInstance> instances = new ArrayList<ServiceInstance>();

        try {
            ConcurrentHashMap<String, List<ServiceInstance>> serviceUpdate
                    = lookupServiceUpdate(subscribeInfo.getGroup());

            if (!serviceUpdate.isEmpty() && serviceUpdate.containsKey(subscribeInfo.getInterfaceName())) {
                instances = serviceUpdate.get(subscribeInfo.getInterfaceName());
            }
        } catch (Exception ex) {
            log.warn("lookup end point list failed from {}, msg={}",
                    url, ex.getMessage());
            if (!subscribeInfo.isIgnoreFailOfNamingService()) {
                throw new RpcException("lookup end point list failed from consul failed", ex);
            }
        }

        return instances;
    }

    @Override public void subscribe(final SubscribeInfo subscribeInfo, final NotifyListener listener) {

        final String path = getSubscribePath(subscribeInfo);

        Future future = heartbeatExecutor.scheduleAtFixedRate(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            long time = System.currentTimeMillis();
                            log.debug("heart beat schedule, time: {}",
                                    time);
                            Map<String, List<ServiceInstance>> instanceForGroup
                                    = serviceCache.get(subscribeInfo.getGroup());
                            List<ServiceInstance> currentInstances = lookup(subscribeInfo);

                            ConcurrentHashMap<String,
                                    List<ServiceInstance>> serviceUpdate = lookupServiceUpdate(
                                    subscribeInfo.getGroup());

                            updateServiceCache(subscribeInfo.getGroup()
                                    , serviceUpdate);

                            log.debug("heart beat schedule, lookup and "
                                            + "update time: {}",
                                    System.currentTimeMillis() - time);

                            if ((instanceForGroup != null && !instanceForGroup.isEmpty())
                                    || !currentInstances.isEmpty()) {
                                List<ServiceInstance> lastInstances = new ArrayList<ServiceInstance>();
                                if (instanceForGroup != null) {
                                    lastInstances = instanceForGroup.get(subscribeInfo.getInterfaceName());
                                }

                                Collection<ServiceInstance> addList = CollectionUtils.subtract(
                                        currentInstances, lastInstances);
                                Collection<ServiceInstance> deleteList = CollectionUtils.subtract(
                                        lastInstances, currentInstances);
                                listener.notify(addList, deleteList);

                                failedSubscribes.remove(subscribeInfo);
                            }
                            log.info("subscribe success from {}", url);
                        } catch (Exception e) {
                            if (!subscribeInfo.isIgnoreFailOfNamingService()) {
                                throw new RpcException("subscribe "
                                        + "failed from " + url, e);
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
        return subscribeInfo.getGroup() + "-" + subscribeInfo.getInterfaceName() + "-" + subscribeInfo.getVersion();
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
        newService.setTags(Arrays.asList(registerInfo.getGroup(), registerInfo.getInterfaceName()));
        NewService.Check check = new NewService.Check();
        check.setTtl(this.consulInterval + "s");
        check.setDeregisterCriticalServiceAfter("3m");

        newService.setCheck(check);

        return newService;
    }

    public String getRegisterPath(RegisterInfo registerInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append(registerInfo.getHost()).append(":").append(registerInfo.getPort()).append("-")
                .append(registerInfo.getInterfaceName());
        return sb.toString();

    }

    private void updateServiceCache(String group, ConcurrentHashMap<String, List<ServiceInstance>> groupUrls) {
        if (groupUrls != null && !groupUrls.isEmpty()) {
            ConcurrentHashMap<String, List<ServiceInstance>> groupMap = serviceCache.get(group);
            if (groupMap == null) {
                serviceCache.put(group, groupUrls);
            }
            for (Map.Entry<String, List<ServiceInstance>> entry : groupUrls.entrySet()) {
                if (groupMap != null) {
                    groupMap.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    private ConcurrentHashMap<String, List<ServiceInstance>> lookupServiceUpdate(String group) {
        ConcurrentHashMap<String, List<ServiceInstance>> groupUrls
                = new ConcurrentHashMap<String, List<ServiceInstance>>();
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
                        List<ServiceInstance> urlList = groupUrls.get(serviceName);

                        if (urlList == null) {
                            urlList = new ArrayList<ServiceInstance>();
                        }
                        urlList.add(new ServiceInstance(service.getAddress(), service.getPort()));
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
