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

import com.baidu.cloud.starlight.api.common.URI;
import com.baidu.cloud.starlight.api.filter.Filter;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.rpc.Invoker;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import com.baidu.cloud.starlight.api.statistics.CountStats;
import com.baidu.cloud.starlight.api.statistics.Stats;
import com.baidu.cloud.starlight.api.transport.PeerStatus;
import com.baidu.cloud.starlight.api.utils.StringUtils;
import com.baidu.cloud.starlight.core.rpc.SingleStarlightClient;
import com.baidu.cloud.starlight.core.statistics.FixedTimeWindowStats;
import com.baidu.cloud.starlight.core.statistics.StarlightStatistics;
import com.baidu.cloud.starlight.core.statistics.StarlightStatsManager;
import com.baidu.cloud.starlight.springcloud.client.cluster.SingleStarlightClientManager;
import com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants.OUTLIER_DETECT_ENABLED;
import static com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants.OUTLIER_DETECT_ENABLED_KEY;
import static com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants.OUTLIER_DETECT_FAIL_COUNT_THRESHOLD_KEY;
import static com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants.OUTLIER_DETECT_FAIL_PERCENT_THRESHOLD;
import static com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants.OUTLIER_DETECT_FAIL_PERCENT_THRESHOLD_KEY;
import static com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants.OUTLIER_DETECT_INTERVAL;
import static com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants.OUTLIER_DETECT_INTERVAL_KEY;
import static com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants.OUTLIER_DETECT_MINI_REQUEST_NUM;
import static com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants.OUTLIER_DETECT_MINI_REQUEST_NUM_KEY;

/**
 * Detect outlier based on statistics, used is client side.
 * <p>
 * Created by liuruisen on 2021/4/20.
 */
public class OutlierDetectFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutlierDetectFilter.class);

    @Override
    public void filterRequest(Invoker invoker, Request request, RpcCallback callback) {
        try {
            configureOutlier(request);
        } catch (Throwable e) {
            LOGGER.warn("OutlierDetectFilter filterRequest failed. ", e);
        }

        // do nothing
        invoker.invoke(request, callback);
    }

    @Override
    public void filterResponse(Response response, Request request) {

        try {
            SingleStarlightClientManager starlightClientManager = SingleStarlightClientManager.getInstance();

            // detect un health outlier & update SingleStarlightClient status
            URI remoteURI = request.getRemoteURI();
            if (remoteURI == null) {
                LOGGER.warn("OutlierFilter cannot find the statistics, because remote uri is null");
                return;
            }

            boolean outlierEnable = remoteURI.getParameter(OUTLIER_DETECT_ENABLED_KEY, OUTLIER_DETECT_ENABLED);
            if (!outlierEnable) {
                return;
            }

            StarlightStatistics statistics = StarlightStatsManager.getStats(remoteURI);
            if (statistics == null) {
                LOGGER.warn("OutlierFilter cannot find the statistics of {} from StarlightStatsManager",
                    remoteURI.getAddress());
                return;
            }

            Stats stats = statistics.discoverStats(SpringCloudConstants.OUTLIER_STATS_KEY);
            if (!(stats instanceof CountStats)) {
                LOGGER.warn("OutlierFilter cannot find the fixed time window statistics of {}", remoteURI.getAddress());
                return;
            }

            CountStats outlierStats = (CountStats) stats;
            Integer outlierMinReqNum = remoteURI.getParameter(OUTLIER_DETECT_MINI_REQUEST_NUM_KEY,
                    OUTLIER_DETECT_MINI_REQUEST_NUM);

            Integer totalReqCount = outlierStats.totalReqCount();
            Integer failReqCount = outlierStats.failReqCount();
            if (totalReqCount < outlierMinReqNum) { // less than outlier detect threshold, return
                return;
            }

            String outlierDetectFailCount = remoteURI.getParameter(OUTLIER_DETECT_FAIL_COUNT_THRESHOLD_KEY);
            if (!StringUtils.isEmpty(outlierDetectFailCount)) {
                int outlierFailCount = Integer.parseInt(outlierDetectFailCount);
                if (failReqCount >= outlierFailCount) {
                    markOutlier(remoteURI, starlightClientManager, outlierStats);
                }
            } else {
                int outlierDetectFailPercent = remoteURI.getParameter(OUTLIER_DETECT_FAIL_PERCENT_THRESHOLD_KEY,
                        OUTLIER_DETECT_FAIL_PERCENT_THRESHOLD);
                int failPercent = failPercent(failReqCount, totalReqCount);
                if (failPercent > outlierDetectFailPercent) { // Failure threshold exceeded, mark client as outlier
                    markOutlier(remoteURI, starlightClientManager, outlierStats);
                }
            }
        } catch (Throwable throwable) {
            LOGGER.warn("OutlierDetectFilter filterResponse failed. ", throwable);
        }

    }

    private void markOutlier(URI remoteURI, SingleStarlightClientManager starlightClientManager,
                             CountStats outlierStats) {
        SingleStarlightClient singleClient =
                starlightClientManager.getSingleClient(remoteURI.getHost(), remoteURI.getPort());
        if (singleClient == null) {
            LOGGER.warn("Statistics of {} is exist in StatisticsManager, "
                            + "but SingleStarlightClient associated with it is not exist in ClientManager",
                    remoteURI.getAddress());
            return;
        }

        // Configured detect fail count, maybe null
        Integer detectFailCount = null;
        String outlierDetectFailCount = remoteURI.getParameter(OUTLIER_DETECT_FAIL_COUNT_THRESHOLD_KEY);
        if (!StringUtils.isEmpty(outlierDetectFailCount)) {
            detectFailCount = Integer.parseInt(outlierDetectFailCount);
        }

        // Configured detect interval
        int detectInterval = remoteURI.getParameter(OUTLIER_DETECT_INTERVAL_KEY, OUTLIER_DETECT_INTERVAL);

        // Configured detect percent, maybe null
        Integer detectFailPercent = null;
        String outlierDetectFailPct = remoteURI.getParameter(OUTLIER_DETECT_FAIL_PERCENT_THRESHOLD_KEY);
        if (!StringUtils.isEmpty(outlierDetectFailPct)) {
            detectFailPercent = Integer.parseInt(outlierDetectFailPct);
        }

        int failPercent = failPercent(outlierStats.failReqCount(), outlierStats.totalReqCount());

        OutlierDetectEvent detectEvent = new OutlierDetectEvent();
        detectEvent.setReqCount(outlierStats.totalReqCount());
        detectEvent.setSuccReqCount(outlierStats.sucReqCount());
        detectEvent.setFailCount(outlierStats.failReqCount());
        detectEvent.setDetectFailCount(detectFailCount);
        detectEvent.setDetectFailPercent(detectFailPercent);
        detectEvent.setFailPercent(failPercent);
        detectEvent.setDetectInterval(detectInterval);

        // update client status to OUTLIER
        PeerStatus peerStatus = new PeerStatus(PeerStatus.Status.OUTLIER, System.currentTimeMillis());
        peerStatus.setStatusReason(detectEvent);
        singleClient.updateStatus(peerStatus);
    }

    private int failPercent(int failReqCount, int totalReqCount) {
        return (int) ((failReqCount * 0.1) / (totalReqCount * 0.1) * 100);
    }

    /**
     * configure outlier
     *
     * @param request
     */
    private void configureOutlier(Request request) {
        URI remoteURI = request.getRemoteURI();
        StarlightStatistics statistics = StarlightStatsManager.getOrCreateStats(remoteURI);
        boolean outlierEnable = remoteURI.getParameter(OUTLIER_DETECT_ENABLED_KEY, OUTLIER_DETECT_ENABLED);
        if (!outlierEnable) {
            return;
        }

        int detectInterval = remoteURI.getParameter(OUTLIER_DETECT_INTERVAL_KEY, OUTLIER_DETECT_INTERVAL);
        if (statistics.discoverStats(SpringCloudConstants.OUTLIER_STATS_KEY) == null) {
            FixedTimeWindowStats outlierStats = new FixedTimeWindowStats(detectInterval);
            statistics.registerStats(SpringCloudConstants.OUTLIER_STATS_KEY, outlierStats);
        }
    }
}
