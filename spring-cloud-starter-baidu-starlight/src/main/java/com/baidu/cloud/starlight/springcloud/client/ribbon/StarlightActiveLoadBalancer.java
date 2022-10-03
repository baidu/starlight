/*
 * Copyright (c) 2019 Baidu, Inc. All Rights Reserved.
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
 
package com.baidu.cloud.starlight.springcloud.client.ribbon;

import com.baidu.cloud.starlight.core.rpc.SingleStarlightClient;
import com.baidu.cloud.starlight.springcloud.client.cluster.SingleStarlightClientManager;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightClientProperties;
import com.baidu.cloud.thirdparty.netty.util.Timeout;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.IPing;
import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.netflix.loadbalancer.ServerListFilter;
import com.netflix.loadbalancer.ServerListUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * StarlightActiveLoadBalancer will return the active server instances based on StarlightClient's status Created by
 * liuruisen on 2020/12/1.
 */
public class StarlightActiveLoadBalancer<T extends Server> extends StarlightAwareDynamicLoadBalancer {

    private static final Logger LOGGER = LoggerFactory.getLogger(StarlightActiveLoadBalancer.class);

    /**
     * 30 min fixme 可配置
     */
    private static final Integer CLEAN_UP_TASK_INIT_DELAY_SECOND = 30 * 60;

    /**
     * 1h fixme 可配置
     */
    private static final Integer OFFLINE_CLIENT_CLEAN_UP_PERIOD = 60 * 60;

    private RibbonServerLocalStore localStore;

    private final SingleStarlightClientManager clientManager;

    private final ScheduledFuture<?> scheduledFuture;

    /**
     * 缓存Ribbon Server的Class类与getMetadata Method的映射，用于加速 只会单线程调用，每个LoadBalancer实现类只会有一个Class
     */
    private static final Map<Class, Method> METADATA_METHODS = new HashMap<>();

    public StarlightActiveLoadBalancer(SingleStarlightClientManager clientManager, IClientConfig config, IRule rule,
        IPing ping, ServerList serverList, ServerListFilter serverListFilter, ServerListUpdater serverListUpdater,
        StarlightClientProperties clientProperties) {
        super(config, rule, ping, serverList, serverListFilter, serverListUpdater, clientProperties);
        this.clientManager = clientManager;
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        scheduledFuture = executorService.scheduleAtFixedRate(new ClientCleanUpTask(), CLEAN_UP_TASK_INIT_DELAY_SECOND,
            OFFLINE_CLIENT_CLEAN_UP_PERIOD, TimeUnit.SECONDS);
    }

    private List<StarlightServerListFilter> serverListFilters;

    @Override
    public List<Server> getReachableServers() {
        return activeServers(super.getReachableServers());
    }

    @Override
    public List<Server> getAllServers() {
        return activeServers(super.getAllServers());
    }

    @Override
    void restOfInit(IClientConfig clientConfig) {
        if (getClientProperties().getLocalCacheEnabled(getName())) {
            localStore = new RibbonServerLocalStore(clientConfig.getClientName(), getClientProperties());
        }
        super.restOfInit(clientConfig);
    }

    @Override
    public void setServersList(List lsrv) {
        List<Server> serverList = (List<Server>) lsrv;
        long startTime = System.currentTimeMillis();
        // 转化为StarlightRibbonServer, 统一各个discovery starter的实现
        List<StarlightRibbonServer> starlightServers = convertToStarlightServer(serverList);
        long cost = System.currentTimeMillis() - startTime;
        if (cost > 5) {
            LOGGER.warn("convertToStarlightServer cost > 5, size {}, cost {}", serverList.size(), cost);
        }
        super.setServersList(starlightServers);
    }

    public void setStarlightServerListFilters(List<StarlightServerListFilter> serverListFilters) {
        this.serverListFilters = serverListFilters;
    }

    public void destroy() {
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
    protected List<Server> activeServers(List<Server> originServers) {

        originServers = updateOrGetCacheServers(originServers);

        if (originServers == null || originServers.size() < 1) {
            return originServers;
        }

        if (serverListFilters == null || serverListFilters.size() < 1) {
            return originServers;
        }

        List<Server> serverList = new LinkedList<>(originServers); // the original servers is unmodifiable, copy once
        for (StarlightServerListFilter serverListFilter : serverListFilters) {
            serverList = serverListFilter.getFilteredList(serverList);
        }

        return serverList;
    }

    private List<Server> updateOrGetCacheServers(List<Server> originServers) {

        if (!getClientProperties().getLocalCacheEnabled(getName()) || localStore == null) {
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

    private List<StarlightRibbonServer> convertToStarlightServer(List<Server> originServers) {
        return originServers.stream().map(server -> {
            StarlightRibbonServer starlightRibbonServer = new StarlightRibbonServer(server.getHost(), server.getPort());
            starlightRibbonServer.setAlive(true);
            starlightRibbonServer.setMetadata(new HashMap<>());
            Class<?> clazz = server.getClass();
            Method metadataMethod = METADATA_METHODS.computeIfAbsent(clazz, aClass -> {
                Method method = null;
                try {
                    method = clazz.getDeclaredMethod("getMetadata");
                } catch (NoSuchMethodException e) {
                    LOGGER.warn("Convert to StarlightRibbonServer failed, cause by NoSuchMethod");
                }
                return method;
            });
            if (metadataMethod != null) {
                Map<String, String> metadata = null;
                try {
                    metadata = (Map<String, String>) metadataMethod.invoke(server);
                    starlightRibbonServer.setMetadata(metadata);
                } catch (Exception e) {
                    LOGGER.warn("Convert to StarlightRibbonServer failed, cause by reflect call failed");
                }
            }
            return starlightRibbonServer;
        }).collect(Collectors.toList());
    }

    /**
     * Clean up unused client
     */
    private class ClientCleanUpTask implements Runnable {
        @Override
        public void run() {
            // fixme 会不会有上线下线周期比较长的服务？
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
