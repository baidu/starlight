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
 
package com.baidu.cloud.starlight.springcloud.client.outlier;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.exception.StarlightRpcException;
import com.baidu.cloud.starlight.api.heartbeat.Heartbeat;
import com.baidu.cloud.starlight.api.heartbeat.HeartbeatRpcRequestHolder;
import com.baidu.cloud.starlight.api.model.ResultFuture;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.api.transport.PeerStatus;
import com.baidu.cloud.starlight.core.rpc.SingleStarlightClient;
import com.baidu.cloud.starlight.core.rpc.callback.FutureCallback;
import com.baidu.cloud.starlight.springcloud.client.cluster.SingleStarlightClientManager;
import com.baidu.cloud.starlight.springcloud.client.properties.OutlierConfig;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightClientProperties;
import com.baidu.cloud.starlight.springcloud.client.cluster.loadbalance.StarlightServerListFilter;
import com.baidu.cloud.starlight.springcloud.common.ClusterLogUtils;
import com.baidu.cloud.starlight.springcloud.common.InstanceUtils;
import com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants;
import com.baidu.cloud.thirdparty.netty.util.Timeout;
import com.baidu.cloud.thirdparty.netty.util.TimerTask;
import org.springframework.cloud.client.ServiceInstance;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants.OUTLIER_SEVER_LIST_FILTER_ORDER;

/**
 * Eject outlier instance
 * <p>
 * Created by liuruisen on 2021/4/22.
 */
public class OutlierEjectServerListFilter implements StarlightServerListFilter {

    private static final Integer OUTLIER_LOG_RECORD_DURATION = 30000;

    private static final String FORCED_RECOVERY_HEALTH = "forced recovery as configured";

    private static final String RECOVERY_HEALTH = "recovery because detected health";

    private static final String RECOVERY_FAIL = "3 consecutive tests failed";

    private final SingleStarlightClientManager singleStarlightClientManager;

    private final StarlightClientProperties clientProperties;

    private final Map<String, Timeout> outlierRecoverTasks;

    private final String clientName;

    public OutlierEjectServerListFilter(SingleStarlightClientManager clientManager,
        StarlightClientProperties clientProperties, String clientName) {
        this.singleStarlightClientManager = clientManager; // not null
        this.clientProperties = clientProperties; // not null
        this.clientName = clientName;
        this.outlierRecoverTasks = new ConcurrentHashMap<>();
    }

    @Override
    public List<ServiceInstance> getFilteredList(List<ServiceInstance> originList) {
        long startTime = System.currentTimeMillis();
        // 修正下异常实例自动恢复任务，应对异常实例重启等情况
        reviseOutlierTask();

        int originSize = originList.size();
        if (originSize <= 1) { // size less than 1, return
            LOGGER.info("Original server instance size is {}, less than or equals 1 will not eject.", originList);
            return originList;
        }

        String clientName = getClientName();
        OutlierConfig outlierConfig = clientProperties.getOutlierConfig(clientName);
        if (!outlierConfig.getEnabled()) { // not enabled, return
            return originList;
        }

        List<ServiceInstance> filtered = new LinkedList<>(originList);
        try {
            double maxEjectCountDouble = (((double) outlierConfig.getMaxEjectPercent() / 100) * originSize);
            if (maxEjectCountDouble < 0.5d) {
                LOGGER.info("Max eject count of {} is 0, max eject double {}, origin size {}, maxEjectPercent {}",
                    getClientName(), maxEjectCountDouble, originSize, outlierConfig.getMaxEjectPercent());
            }
            int maxEjectCount = (int) Math.round(maxEjectCountDouble);
            int ejectCount = 0;
            for (ServiceInstance server : originList) {
                if (ejectCount >= maxEjectCount) {
                    LOGGER.info("Reach the max eject count {}, will not eject.", maxEjectCount);
                    break;
                }

                SingleStarlightClient client =
                    singleStarlightClientManager.getSingleClient(server.getHost(), server.getPort());
                // not use this server instance, not filter
                if (client == null) {
                    continue;
                }

                if (client.getStatus() == null) {
                    continue;
                }

                // outlier, remove from the server list
                if (PeerStatus.Status.OUTLIER.equals(client.getStatus().getStatus())) {
                    filtered.remove(server);
                    ejectCount++;
                    if ((System.currentTimeMillis()
                        - client.getStatus().getStatusRecordTime()) < OUTLIER_LOG_RECORD_DURATION) {
                        ClusterLogUtils.logOutlierInstanceEject(LOGGER, getClientName(), server, client.getStatus());
                    }
                    // detect recover
                    submitTimerTask(server, outlierConfig.getBaseEjectTime());
                }
            }
            if (ejectCount > 0) {
                // 记录本provider当前被摘除的实例个数
                ClusterLogUtils.logOutlierAppEject(LOGGER, getClientName(), originSize, ejectCount, maxEjectCount);
            }
        } catch (Throwable e) {
            LOGGER.warn("OutlierEjectServerListFilter getFilteredList failed, will use all instances, " + "caused by ",
                e);
        }
        LOGGER.debug("OutlierEjectServerListFilter getFilteredList cost {}", System.currentTimeMillis() - startTime);
        return filtered;
    }

    @Override
    public SingleStarlightClientManager getSingleClientManager() {
        return this.singleStarlightClientManager;
    }

    @Override
    public int getOrder() {
        return OUTLIER_SEVER_LIST_FILTER_ORDER;
    }

    @Override
    public Map<String, Timeout> getServerListFilterTasks() {
        return this.outlierRecoverTasks;
    }

    @Override
    public synchronized void submitTimerTask(ServiceInstance server, Integer ejectTime) {
        String clientName = getClientName();
        OutlierConfig outlierConfig = clientProperties.getOutlierConfig(clientName);

        if (outlierRecoverTasks.get(InstanceUtils.ipPortStr(server)) != null
            && outlierConfig.getBaseEjectTime().equals(ejectTime)) {
            return;
        }

        LOGGER.debug("Add new detect timer server {}, eject time {}s", InstanceUtils.ipPortStr(server), ejectTime);
        Timeout timeout = SERVER_LIST_FILTER_TIMER.newTimeout(new OutlierRecoverTask(server, ejectTime, outlierConfig),
            ejectTime, TimeUnit.SECONDS);
        outlierRecoverTasks.put(InstanceUtils.ipPortStr(server), timeout);
    }

    @Override
    public void destroy() {
        if (outlierRecoverTasks.size() > 0) {
            for (Map.Entry<String, Timeout> task : outlierRecoverTasks.entrySet()) {
                if (task != null && task.getValue() != null) {
                    task.getValue().cancel();
                }
            }
        }
    }

    private void reviseOutlierTask() {
        if (outlierRecoverTasks.isEmpty()) {
            return;
        }
        Map<String, Timeout> readOnlyTasks = Collections.unmodifiableMap(outlierRecoverTasks);
        for (Map.Entry<String, Timeout> entry : readOnlyTasks.entrySet()) {
            try {
                String[] hostPort = entry.getKey().split(":");
                String host = hostPort[0];
                String port = hostPort[1];
                SingleStarlightClient client =
                    singleStarlightClientManager.getSingleClient(host, Integer.valueOf(port));
                // client为空，表示这个应用已经下线清理，修正下异常实例恢复任务
                if (client == null) {
                    Timeout timeout = outlierRecoverTasks.remove(entry.getKey());
                    timeout.cancel();
                    continue;
                }
                // 非OUTLIER状态，重启了或者重新上线，修正下异常实例恢复任务
                if (!PeerStatus.Status.OUTLIER.equals(client.getStatus().getStatus())) {
                    Timeout timeout = outlierRecoverTasks.remove(entry.getKey());
                    timeout.cancel();
                }
            } catch (Exception e) {
                LOGGER.warn("Revise outlier task failed, hostPort {}, errorMsg {}", entry.getKey(), e.getMessage());
            }
        }
    }

    private String getClientName() {
        return clientName;
    }

    private class OutlierRecoverTask implements TimerTask {

        private final ServiceInstance server;

        private final OutlierConfig outlierConfig;

        private final Integer lastEjectTime;

        public OutlierRecoverTask(ServiceInstance server, Integer lastEjectTime, OutlierConfig outlierConfig) {
            this.server = server;
            this.outlierConfig = outlierConfig;
            this.lastEjectTime = lastEjectTime;
        }

        @Override
        public void run(Timeout timeout) throws Exception {
            SingleStarlightClient singleClient =
                singleStarlightClientManager.getSingleClient(server.getHost(), server.getPort());
            if (singleClient == null) { // removed client, the service instance is offline
                return;
            }

            if (!PeerStatus.Status.OUTLIER.equals(singleClient.getStatus().getStatus())) {
                return; // the status is not OUTLIER, return
            }

            if (!outlierConfig.getRecoverByCheckEnabled()) {
                singleClient.updateStatus(new PeerStatus(PeerStatus.Status.ACTIVE, System.currentTimeMillis()));
                getServerListFilterTasks().remove(InstanceUtils.ipPortStr(server));
                ClusterLogUtils.logOutlierRecoverySucc(LOGGER, getClientName(), server, FORCED_RECOVERY_HEALTH,
                    lastEjectTime);
                return;
            }

            // request heartbeat
            ServiceConfig serviceConfig = new ServiceConfig();
            serviceConfig.setInvokeTimeoutMills(SpringCloudConstants.HEARTBEAT_REQUEST_TIMEOUT);

            int heartbeatSucCount = 0;
            for (int i = 0; i < 3; i++) { // try detect 3 times
                RpcRequest heartbeatReq = HeartbeatRpcRequestHolder.heartbeatRequest();
                heartbeatReq.setServiceConfig(serviceConfig);
                try {
                    ResultFuture resultFuture = new ResultFuture();
                    RpcCallback rpcCallback = new FutureCallback(resultFuture, heartbeatReq);
                    singleClient.request(heartbeatReq, rpcCallback);
                    Object result = resultFuture.get();

                    if (result instanceof Heartbeat && Constants.PONG.equals(((Heartbeat) result).getMessage())) {
                        heartbeatSucCount++;
                    } else {
                        LOGGER.warn("Outlier recover heartbeat receive response from {} success, "
                            + "but message is not correct {}", InstanceUtils.ipPortStr(server), result);
                    }
                } catch (Throwable exp) {
                    if (exp instanceof StarlightRpcException) {
                        StarlightRpcException rpcException = (StarlightRpcException) exp;
                        // Compatible with the old version
                        // Old version starlight server will return SERVICE_NOT_FOUND_EXCEPTION
                        // because it dose not export HeartbeatService
                        if (StarlightRpcException.SERVICE_NOT_FOUND_EXCEPTION.equals(rpcException.getCode())) {
                            heartbeatSucCount++;
                        }
                    }
                }
            }

            if (heartbeatSucCount >= 3) { // success
                singleClient.updateStatus(new PeerStatus(PeerStatus.Status.ACTIVE, System.currentTimeMillis()));
                getServerListFilterTasks().remove(InstanceUtils.ipPortStr(server));
                ClusterLogUtils.logOutlierRecoverySucc(LOGGER, getClientName(), server, RECOVERY_HEALTH, lastEjectTime);
            } else { // fail
                // Accumulate the detection time of recover
                int nextEjectTime = lastEjectTime + outlierConfig.getBaseEjectTime();
                if (nextEjectTime > outlierConfig.getMaxEjectTime()) {
                    nextEjectTime = outlierConfig.getMaxEjectTime();
                }
                ClusterLogUtils.logOutlierRecoveryFail(LOGGER, getClientName(), server, RECOVERY_FAIL, lastEjectTime,
                    nextEjectTime);
                // resubmit task
                submitTimerTask(server, nextEjectTime);
            }
        }
    }

}
