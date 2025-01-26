package com.baidu.cloud.starlight.springcloud.client.cluster.loadbalance;

import com.baidu.cloud.starlight.core.rpc.SingleStarlightClient;
import com.baidu.cloud.starlight.springcloud.client.cluster.SingleStarlightClientManager;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightClientProperties;
import com.baidu.cloud.thirdparty.netty.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.loadbalancer.core.DelegatingServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.ConfigurableApplicationContext;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 每个下游对应一个ServiceInstanceListSupplier
 * 在此处实现 异常实例摘除、shutdown实例摘除、路由实例筛选逻辑
 * @Date 2022/12/9 15:13
 * @Created by liuruisen
 */
public class StarlightActiveServiceInstanceListSupplier extends DelegatingServiceInstanceListSupplier {

    private static final Logger LOGGER = LoggerFactory.getLogger(StarlightActiveServiceInstanceListSupplier.class);

    /**
     * 30 min
     */
    private static final Integer CLEAN_UP_TASK_INIT_DELAY_SECOND = 30 * 60;

    /**
     * 1h
     */
    private static final Integer OFFLINE_CLIENT_CLEAN_UP_PERIOD = 60 * 60;

    private StarlightClientProperties clientProperties;

    private ServiceInstanceLocalStore localStore;

    private final SingleStarlightClientManager clientManager;

    private List<StarlightServerListFilter> serverListFilters;


    private ScheduledFuture<?> scheduledFuture;

    public StarlightActiveServiceInstanceListSupplier(ConfigurableApplicationContext context,
                                                      ServiceInstanceListSupplier delegate) {
        super(delegate);
        this.clientProperties = context.getBean(StarlightClientProperties.class);
        this.clientManager = context.getBean(SingleStarlightClientManager.class);
        Map<String, StarlightServerListFilter> filterMap = context.getBeansOfType(StarlightServerListFilter.class);
        if (filterMap != null && !filterMap.isEmpty()) {
            this.serverListFilters = filterMap.values().stream().toList();
        } else {
            this.serverListFilters = new ArrayList<>();
        }
    }

    @Override
    public Flux<List<ServiceInstance>> get() {
        return delegate.get().map(serviceInstances -> activeServers(serviceInstances));
    }

    @Override
    public Flux<List<ServiceInstance>> get(Request request) {
        return get();
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();

        if (clientProperties.getLocalCacheEnabled(getServiceId())) {
            localStore = new ServiceInstanceLocalStore(getServiceId(), clientProperties);
        }
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        scheduledFuture = executorService.scheduleAtFixedRate(new ClientCleanUpTask(),
                CLEAN_UP_TASK_INIT_DELAY_SECOND, OFFLINE_CLIENT_CLEAN_UP_PERIOD, TimeUnit.SECONDS);
    }


    @Override
    public void destroy() throws Exception {
        super.destroy();
        if (scheduledFuture != null && !scheduledFuture.isCancelled()) {
            scheduledFuture.cancel(true);
        }

        if (localStore != null) {
            localStore.close();
            localStore = null; // 防止海若逻辑二次关闭
        }

        for (StarlightServerListFilter serverListFilter : serverListFilters) {
            serverListFilter.destroy();
        }
    }

    /**
     * Get active servers
     *
     * @param originServers
     * @return
     */
    protected List<ServiceInstance> activeServers(List<ServiceInstance> originServers) {

        originServers = updateOrGetCacheServers(originServers);

        if (originServers == null || originServers.size() < 1) {
            return originServers;
        }

        if (serverListFilters == null || serverListFilters.size() < 1) {
            return originServers;
        }

        List<ServiceInstance> serverList = new LinkedList<>(originServers); // the original servers is unmodifiable, copy once
        for (StarlightServerListFilter serverListFilter : serverListFilters) {
            serverList = serverListFilter.getFilteredList(serverList);
        }

        return serverList;
    }

    private List<ServiceInstance> updateOrGetCacheServers(List<ServiceInstance> originServers) {

        if (!clientProperties.getLocalCacheEnabled(getServiceId())
                || localStore == null) {
            return originServers;
        }

        try {
            if (originServers == null || originServers.size() == 0) {
                // get servers from local cache
                return localStore.getCachedListOfServers();
            } else {
                // update cache
                localStore.updateCachedListOfServers(originServers);
                return originServers;
            }
        } catch (Throwable e) {
            LOGGER.warn("Update or get cached servers failed, caused by ", e);
        }

        return originServers;
    }

    /**
     * Clean up unused client
     */
    private class ClientCleanUpTask implements Runnable {
        @Override
        public void run() {
            for (Map.Entry<String, SingleStarlightClient> entry : clientManager.allSingleClients().entrySet()) {
                if (!entry.getValue().isActive()) {
                    long inactiveDuration =
                            System.currentTimeMillis() - entry.getValue().getStatus().getStatusRecordTime();
                    if (inactiveDuration >= OFFLINE_CLIENT_CLEAN_UP_PERIOD * 1000) {
                        String clientId = entry.getKey();
                        String[] ipPort = clientId.split(":");
                        LOGGER.info("StarlightActiveLoadBalancer detects that remote {} "
                                + "has not been used for 2h, will remove from ClientManager", clientId);
                        clientManager.removeSingleClient(ipPort[0], Integer.valueOf(ipPort[1]));
                        for (StarlightServerListFilter filter : serverListFilters) {
                            if (filter.getServerListFilterTasks() == null) {
                                continue;
                            }
                            Object timeout = filter.getServerListFilterTasks().get(clientId);
                            if (timeout instanceof Timeout) {
                                LOGGER.info("StarlightActiveLoadBalancer detects that remote {} "
                                        + "has not been used for 2h, will cancel the tasks", clientId);
                                ((Timeout) timeout).cancel();
                            }
                        }
                    }
                }
            }
        }
    }
}
