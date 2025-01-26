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
 
package com.baidu.cloud.starlight.springcloud.client.shutdown;

import com.baidu.cloud.starlight.api.transport.PeerStatus;
import com.baidu.cloud.starlight.api.utils.StringUtils;
import com.baidu.cloud.starlight.core.rpc.SingleStarlightClient;
import com.baidu.cloud.starlight.core.statistics.StarlightStatsManager;
import com.baidu.cloud.starlight.springcloud.client.cluster.SingleStarlightClientManager;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightClientProperties;
import com.baidu.cloud.starlight.springcloud.client.cluster.loadbalance.StarlightServerListFilter;
import com.baidu.cloud.starlight.springcloud.common.InstanceUtils;
import com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants;
import com.baidu.cloud.thirdparty.netty.util.Timeout;
import com.baidu.cloud.thirdparty.netty.util.TimerTask;
import org.springframework.cloud.client.ServiceInstance;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Remove shutdown instance filter
 * <p>
 * Created by liuruisen on 2021/4/23.
 */
public class ShutdownServerListFilter implements StarlightServerListFilter {

    /**
     * 30 min
     */
    private static final Integer SHUTDOWN_CLEAN_UP_PERIOD = 30 * 60 * 1000;

    private final SingleStarlightClientManager clientManager;

    private final StarlightClientProperties clientProperties;

    private final Map<String, Timeout> shutdownCleanTasks;

    public ShutdownServerListFilter(SingleStarlightClientManager clientManager,
        StarlightClientProperties clientProperties) {
        this.clientManager = clientManager;
        this.clientProperties = clientProperties;
        this.shutdownCleanTasks = new ConcurrentHashMap<>();
    }

    @Override
    public List<ServiceInstance> getFilteredList(List<ServiceInstance> originServers) {

        long filterStartTime = System.currentTimeMillis();

        if (originServers == null || originServers.isEmpty()) {
            return originServers;
        }

        // Since the original servers is unmodifiable, we copy once
        List<ServiceInstance> serverList = new LinkedList<>(originServers);

        for (ServiceInstance server : originServers) {
            Map<String, String> serverMetas = server.getMetadata();

            if (serverMetas != null && serverMetas.size() > 0) {

                SingleStarlightClient starlightClient =
                    clientManager.getSingleClient(server.getHost(), server.getPort());
                // starlight client is null, this client is not used in a long period
                // have the right to be selected by load balancing
                if (starlightClient == null) {
                    continue;
                }

                // epoch empty continue
                String epoch = serverMetas.get(SpringCloudConstants.EPOCH_KEY);
                if (StringUtils.isEmpty(epoch)) {
                    LOGGER.debug("The server discovered by registry does not have EPOCH meta!");
                    continue;
                }

                long epochTime = Long.parseLong(epoch);

                // peer status null, continue
                PeerStatus peerStatus = starlightClient.getStatus();
                if (peerStatus == null) {
                    continue;
                }

                // epoch > in active time, the server is reboot

                // starlight client is not shutdown, have the right to be selected by load balancing
                if (!PeerStatus.Status.SHUTTING_DOWN.equals(peerStatus.getStatus())
                    && !PeerStatus.Status.SHUTDOWN.equals(peerStatus.getStatus())) {
                    continue;
                }
                long shutdownTime = peerStatus.getStatusRecordTime();

                // The service is inactive and has no right to be selected by the load balancer
                if (epochTime < shutdownTime) {
                    serverList.remove(server);
                    LOGGER.info("Remote server {} had been removed because of shutdown, shutdownTime {}.",
                        InstanceUtils.ipPortStr(server), peerStatus.getStatusRecordTime());
                    submitTimerTask(server, SHUTDOWN_CLEAN_UP_PERIOD);
                }
            }
        }
        LOGGER.debug("ShutdownServerListFilter getFilteredList cost {}", System.currentTimeMillis() - filterStartTime);

        return serverList;
    }

    @Override
    public Map<String, Timeout> getServerListFilterTasks() {
        return this.shutdownCleanTasks;
    }

    @Override
    public SingleStarlightClientManager getSingleClientManager() {
        return this.clientManager;
    }

    @Override
    public int getOrder() {
        return SpringCloudConstants.SHUTTING_DOWN_SERVER_LIST_FILTER_ORDER;
    }

    @Override
    public void submitTimerTask(ServiceInstance server, Integer execDelay) {
        if (shutdownCleanTasks.get(InstanceUtils.ipPortStr(server)) != null) {
            return;
        }
        Timeout timeout =
            SERVER_LIST_FILTER_TIMER.newTimeout(new ShutdownInstanceCleanTask(server), execDelay, TimeUnit.SECONDS);
        shutdownCleanTasks.put(InstanceUtils.ipPortStr(server), timeout);
    }

    @Override
    public void destroy() {
        if (shutdownCleanTasks.size() > 0) {
            for (Map.Entry<String, Timeout> task : shutdownCleanTasks.entrySet()) {
                if (task != null && task.getValue() != null) {
                    task.getValue().cancel();
                }
            }
        }
    }

    private class ShutdownInstanceCleanTask implements TimerTask {

        private final ServiceInstance server;

        public ShutdownInstanceCleanTask(ServiceInstance server) {
            this.server = server;
        }

        @Override
        public void run(Timeout timeout) throws Exception {
            SingleStarlightClient singleClient = clientManager.getSingleClient(server.getHost(), server.getPort());
            if (singleClient == null) {
                return;
            }

            PeerStatus peerStatus = singleClient.getStatus();
            if (peerStatus == null) {
                return;
            }

            if (!PeerStatus.Status.SHUTTING_DOWN.equals(peerStatus.getStatus())
                || !PeerStatus.Status.SHUTDOWN.equals(peerStatus.getStatus())) {
                return;
            }

            if ((System.currentTimeMillis() - peerStatus.getStatusRecordTime()) >= SHUTDOWN_CLEAN_UP_PERIOD) {
                StarlightStatsManager.removeStats(singleClient.remoteURI());
                clientManager.removeSingleClient(server.getHost(), server.getPort());
            }
        }
    }
}
