package com.baidu.brpc.naming;

import com.baidu.brpc.client.CommunicationClient;
import com.baidu.brpc.client.CommunicationOptions;
import com.baidu.brpc.client.InterceptCommunicationClient;
import com.baidu.brpc.client.channel.BootstrapManager;
import com.baidu.brpc.client.channel.Endpoint;
import com.baidu.brpc.client.channel.ServiceInstance;
import com.baidu.brpc.protocol.NamingOptions;
import com.baidu.brpc.protocol.SubscribeInfo;
import com.baidu.brpc.thread.BrpcThreadPoolManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Getter
public class NamingServiceProcessor {
    private NamingService namingService;
    private Class serviceInterface;
    private SubscribeInfo subscribeInfo;
    private CopyOnWriteArrayList<ServiceInstance> allInstances
            = new CopyOnWriteArrayList<ServiceInstance>();
    private CopyOnWriteArrayList<CommunicationClient> healthyInstances
            = new CopyOnWriteArrayList<CommunicationClient>();
    private CopyOnWriteArrayList<CommunicationClient> unhealthyInstances
            = new CopyOnWriteArrayList<CommunicationClient>();
    private Lock lock = new ReentrantLock();
    private HealthyCheckTimer healthyCheckTimer;
    private CommunicationOptions communicationOptions;

    public NamingServiceProcessor(String serviceUrl, Class serviceInterface,
                                  NamingOptions namingOptions,
                                  int healthyCheckIntervalMillis,
                                  CommunicationOptions communicationOptions) {
        this.communicationOptions = communicationOptions;
        this.serviceInterface = serviceInterface;
        // parse naming
        BrpcURL url = new BrpcURL(serviceUrl);
        NamingServiceFactory namingServiceFactory
                = NamingServiceFactoryManager.getInstance().getNamingServiceFactory(url.getSchema());
        this.namingService = namingServiceFactory.createNamingService(url);

        if (namingOptions != null) {
            subscribeInfo = new SubscribeInfo(namingOptions);
        } else {
            subscribeInfo = new SubscribeInfo();
        }
        subscribeInfo.setInterfaceName(serviceInterface.getName());

        List<ServiceInstance> instances = this.namingService.lookup(subscribeInfo);
        addInstances(instances);
        if (!(namingService instanceof ListNamingService)) {
            this.namingService.subscribe(subscribeInfo, new NotifyListener() {
                @Override
                public void notify(Collection<ServiceInstance> addList,
                                   Collection<ServiceInstance> deleteList) {
                    log.info("receive {} added instances, {} deleted instances from naming service",
                            addList.size(), deleteList.size());
                    addInstances(addList);
                    deleteInstances(deleteList);
                }
            });
        }
        // starter healthy check timer
        startHealthyCheckTimer(healthyCheckIntervalMillis);
    }

    public NamingServiceProcessor(List<Endpoint> endpoints,
                                  Class serviceInterface,
                                  int healthyCheckIntervalMillis,
                                  CommunicationOptions communicationOptions) {
        this.serviceInterface = serviceInterface;
        this.communicationOptions = communicationOptions;
        subscribeInfo = new SubscribeInfo();
        subscribeInfo.setInterfaceName(serviceInterface.getName());
        for (Endpoint endpoint : endpoints) {
            ServiceInstance instance = new ServiceInstance(endpoint);
            instance.setServiceName(subscribeInfo.getServiceId());
            addInstance(instance);
        }
        if (allInstances.size() > 1) {
            // starter healthy check timer
            startHealthyCheckTimer(healthyCheckIntervalMillis);
        }
    }

    private void startHealthyCheckTimer(int healthyCheckIntervalMillis) {
        healthyCheckTimer = new HealthyCheckTimer(this, healthyCheckIntervalMillis);
        healthyCheckTimer.start();
    }

    public void addInstances(Collection<ServiceInstance> instances) {
        for (ServiceInstance instance : instances) {
            addInstance(instance);
        }
    }

    public void addInstance(ServiceInstance instance) {
        lock.lock();
        try {
            if (!allInstances.contains(instance)) {
                allInstances.add(instance);
                CommunicationClient communicationClient = new InterceptCommunicationClient(
                        instance, communicationOptions, communicationOptions.getInterceptors());
                healthyInstances.add(communicationClient);
            } else {
                log.debug("service instance already exist, {}:{}", instance.getIp(), instance.getPort());
            }
        } finally {
            lock.unlock();
        }
    }

    public void deleteInstances(Collection<ServiceInstance> instances) {
        List<CommunicationClient> removedClients = new ArrayList<CommunicationClient>();
        for (ServiceInstance instance : instances) {
            CommunicationClient communicationClient = deleteInstance(instance);
            if (communicationClient != null) {
                removedClients.add(communicationClient);
            }
        }

        // close the channel pool after 1 second, so that request can be finished
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            log.warn("InterruptedException:", ex);
        }
        for (CommunicationClient communicationClient : removedClients) {
            communicationClient.stop();
        }
    }

    public CommunicationClient deleteInstance(ServiceInstance instance) {
        lock.lock();
        try {
            if (allInstances.remove(instance)) {
                CommunicationClient communicationClient = deleteInstance(healthyInstances, instance);
                if (communicationClient == null) {
                    communicationClient = deleteInstance(unhealthyInstances, instance);
                }
                if (communicationClient == null) {
                    log.warn("instance {} exist in allInstances, " +
                            "but not in healthyInstances and unhealthyInstances", instance);
                }
                return communicationClient;
            }
        } catch (Exception e){
            log.error("delete instance {}, error msg {}", instance, e.getMessage());
        } finally {
            lock.unlock();
        }
        return null;
    }

    public List<CommunicationClient> getInstances() {
        List<CommunicationClient> instances = new ArrayList<CommunicationClient>();
        if (healthyInstances.size() > 0) {
            instances.addAll(healthyInstances);
        } else {
            instances.addAll(unhealthyInstances);
        }
        return instances;
    }

    public void stop() {
        if (healthyCheckTimer != null) {
            healthyCheckTimer.stop();
        }
        if (namingService != null && !(namingService instanceof ListNamingService)) {
            namingService.unsubscribe(subscribeInfo);
            namingService.destroy();
        }
        for (CommunicationClient client : healthyInstances) {
            client.stop();
        }
        for (CommunicationClient client : unhealthyInstances) {
            client.stop();
        }
        if (!communicationOptions.isGlobalThreadPoolSharing()) {
            BrpcThreadPoolManager threadPoolManager = BrpcThreadPoolManager.getInstance();
            threadPoolManager.stopServiceThreadPool(subscribeInfo.getServiceId());
        }
        BootstrapManager bootstrapManager = BootstrapManager.getInstance();
        bootstrapManager.removeBootstrap(subscribeInfo.getServiceId());

    }

    private CommunicationClient deleteInstance(
            CopyOnWriteArrayList<CommunicationClient> list, ServiceInstance item) {
        CommunicationClient instance = null;
        Iterator<CommunicationClient> iterator = list.iterator();
        while (iterator.hasNext()) {
            CommunicationClient toCheck = iterator.next();
            if (toCheck.getServiceInstance().equals(item)) {
                instance = toCheck;
                list.remove(instance);
                break;
            }
        }
        return instance;
    }

}
