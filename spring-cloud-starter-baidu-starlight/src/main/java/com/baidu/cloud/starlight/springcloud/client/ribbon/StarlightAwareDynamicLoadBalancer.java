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

import com.baidu.cloud.starlight.springcloud.client.properties.StarlightClientProperties;
import com.netflix.client.ClientFactory;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.AbstractServerList;
import com.netflix.loadbalancer.AbstractServerListFilter;
import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.IPing;
import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.netflix.loadbalancer.ServerListFilter;
import com.netflix.loadbalancer.ServerListUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A copy of {@link com.netflix.loadbalancer.DynamicServerListLoadBalancer} 增加可感知到 {@link StarlightClientProperties}的逻辑，
 * 执行{@link StarlightAwareDynamicLoadBalancer#restOfInit(IClientConfig)}时可感知到Starlight的配置信息 Created by liuruisen on
 * 2021/6/25.
 */
public class StarlightAwareDynamicLoadBalancer<T extends Server> extends BaseLoadBalancer {

    private static final Logger LOGGER = LoggerFactory.getLogger(StarlightAwareDynamicLoadBalancer.class);

    // to keep track of modification of server lists
    protected AtomicBoolean serverListUpdateInProgress = new AtomicBoolean(false);

    volatile ServerList<T> serverListImpl;

    volatile ServerListFilter<T> filter;

    private StarlightClientProperties clientProperties;

    protected final ServerListUpdater.UpdateAction updateAction = new ServerListUpdater.UpdateAction() {
        @Override
        public void doUpdate() {
            updateListOfServers();
        }
    };

    protected volatile ServerListUpdater serverListUpdater;

    public StarlightAwareDynamicLoadBalancer(IClientConfig clientConfig, IRule rule, IPing ping,
        ServerList<T> serverList, ServerListFilter<T> filter, ServerListUpdater serverListUpdater,
        StarlightClientProperties clientProperties) {
        super(clientConfig, rule, ping);
        this.clientProperties = clientProperties;
        this.serverListImpl = serverList;
        this.filter = filter;
        this.serverListUpdater = serverListUpdater;
        if (filter instanceof AbstractServerListFilter) {
            ((AbstractServerListFilter) filter).setLoadBalancerStats(getLoadBalancerStats());
        }
        restOfInit(clientConfig);
    }

    @Override
    public void initWithNiwsConfig(IClientConfig clientConfig) {
        try {
            super.initWithNiwsConfig(clientConfig);
            String niwsServerListClassName = clientConfig.getPropertyAsString(
                CommonClientConfigKey.NIWSServerListClassName, DefaultClientConfigImpl.DEFAULT_SEVER_LIST_CLASS);

            ServerList<T> niwsServerListImpl = (ServerList<T>) ClientFactory
                .instantiateInstanceWithClientConfig(niwsServerListClassName, clientConfig);
            this.serverListImpl = niwsServerListImpl;

            if (niwsServerListImpl instanceof AbstractServerList) {
                AbstractServerListFilter<T> niwsFilter =
                    ((AbstractServerList) niwsServerListImpl).getFilterImpl(clientConfig);
                niwsFilter.setLoadBalancerStats(getLoadBalancerStats());
                this.filter = niwsFilter;
            }

            String serverListUpdaterClassName =
                clientConfig.getPropertyAsString(CommonClientConfigKey.ServerListUpdaterClassName,
                    DefaultClientConfigImpl.DEFAULT_SERVER_LIST_UPDATER_CLASS);

            this.serverListUpdater = (ServerListUpdater) ClientFactory
                .instantiateInstanceWithClientConfig(serverListUpdaterClassName, clientConfig);

            restOfInit(clientConfig);
        } catch (Exception e) {
            throw new RuntimeException("Exception while initializing NIWSDiscoveryLoadBalancer:"
                + clientConfig.getClientName() + ", niwsClientConfig:" + clientConfig, e);
        }
    }

    void restOfInit(IClientConfig clientConfig) {
        boolean primeConnection = this.isEnablePrimingConnections();
        // turn this off to avoid duplicated asynchronous priming done in BaseLoadBalancer.setServerList()
        this.setEnablePrimingConnections(false);
        enableAndInitLearnNewServersFeature();
        // for hairuo, 允许海若初始化时不拉取服务列表
        if (clientProperties.getFetchInstancesWhenInitEnabled(getName())) {
            updateListOfServers();
        }
        if (primeConnection && this.getPrimeConnections() != null) {
            this.getPrimeConnections().primeConnections(getReachableServers());
        }
        this.setEnablePrimingConnections(primeConnection);
        LOGGER.info("DynamicServerListLoadBalancer for client {} initialized: {}", clientConfig.getClientName(),
            this.toString());
    }

    @Override
    public void setServersList(List lsrv) {
        super.setServersList(lsrv);
        List<T> serverList = (List<T>) lsrv;
        Map<String, List<Server>> serversInZones = new HashMap<String, List<Server>>();
        for (Server server : serverList) {
            // make sure ServerStats is created to avoid creating them on hot
            // path
            getLoadBalancerStats().getSingleServerStat(server);
            String zone = server.getZone();
            if (zone != null) {
                zone = zone.toLowerCase();
                List<Server> servers = serversInZones.get(zone);
                if (servers == null) {
                    servers = new ArrayList<Server>();
                    serversInZones.put(zone, servers);
                }
                servers.add(server);
            }
        }
        setServerListForZones(serversInZones);
    }

    protected void setServerListForZones(Map<String, List<Server>> zoneServersMap) {
        LOGGER.debug("Setting server list for zones: {}", zoneServersMap);
        getLoadBalancerStats().updateZoneServerMapping(zoneServersMap);
    }

    public ServerList<T> getServerListImpl() {
        return serverListImpl;
    }

    public void setServerListImpl(ServerList<T> niwsServerList) {
        this.serverListImpl = niwsServerList;
    }

    public ServerListFilter<T> getFilter() {
        return filter;
    }

    public void setFilter(ServerListFilter<T> filter) {
        this.filter = filter;
    }

    public ServerListUpdater getServerListUpdater() {
        return serverListUpdater;
    }

    public void setServerListUpdater(ServerListUpdater serverListUpdater) {
        this.serverListUpdater = serverListUpdater;
    }

    /**
     * Makes no sense to ping an inmemory disc client
     *
     */
    @Override
    public void forceQuickPing() {
        // no-op
    }

    /**
     * Feature that lets us add new instances (from AMIs) to the list of existing servers that the LB will use Call this
     * method if you want this feature enabled
     */
    public void enableAndInitLearnNewServersFeature() {
        if (serverListUpdater != null) { // for hairuo, serverListUpdater maybe null
            LOGGER.info("Using serverListUpdater {}", serverListUpdater.getClass().getSimpleName());
            serverListUpdater.start(updateAction);
        }
    }

    private String getIdentifier() {
        return this.getClientConfig().getClientName();
    }

    public void stopServerListRefreshing() {
        if (serverListUpdater != null) {
            serverListUpdater.stop();
        }
    }

    public void updateListOfServers() {
        List<T> servers = new ArrayList<T>();
        if (serverListImpl != null) {
            servers = serverListImpl.getUpdatedListOfServers();
            LOGGER.debug("List of Servers for {} obtained from Discovery client: {}", getIdentifier(), servers);

            if (filter != null) {
                servers = filter.getFilteredListOfServers(servers);
                LOGGER.debug("Filtered List of Servers for {} obtained from Discovery client: {}", getIdentifier(),
                    servers);
            }
        }
        updateAllServerList(servers);
    }

    /**
     * Update the AllServer list in the LoadBalancer if necessary and enabled
     *
     * @param ls
     */
    protected void updateAllServerList(List<T> ls) {
        // other threads might be doing this - in which case, we pass
        if (serverListUpdateInProgress.compareAndSet(false, true)) {
            try {
                for (T s : ls) {
                    s.setAlive(true); // set so that clients can start using these
                    // servers right away instead
                    // of having to wait out the ping cycle.
                }
                setServersList(ls);
                super.forceQuickPing();
            } finally {
                serverListUpdateInProgress.set(false);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DynamicServerListLoadBalancer:");
        sb.append(super.toString());
        sb.append("ServerList:" + String.valueOf(serverListImpl));
        return sb.toString();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        stopServerListRefreshing();
    }

    public StarlightClientProperties getClientProperties() {
        return clientProperties;
    }

    public void setClientProperties(StarlightClientProperties clientProperties) {
        this.clientProperties = clientProperties;
    }
}
