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
 
package com.baidu.cloud.starlight.core.filter;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.filter.Filter;
import com.baidu.cloud.starlight.api.heartbeat.HeartbeatService;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.rpc.Invoker;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import com.baidu.cloud.starlight.api.utils.LogUtils;
import com.baidu.cloud.starlight.core.statistics.StarlightStatistics;
import com.baidu.cloud.starlight.core.statistics.StarlightStatsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Client Monitor Filter: used by eye probe agent SPI: clientmonitor Created by liuruisen on 2020/4/20.
 */
public class ClientMonitorFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientMonitorFilter.class);

    @Override
    public void filterRequest(Invoker invoker, Request request, RpcCallback callback) {
        try {
            Map<String, String> traceSpanId = LogUtils.parseTraceIdSpanId(request);
            request.getAttachmentKv().put(Constants.TRACE_ID_KEY, traceSpanId.get(LogUtils.TCID));
            request.getAttachmentKv().put(Constants.SPAN_ID_KEY, traceSpanId.get(LogUtils.SPID));
            request.getAttachmentKv().put(Constants.X_B3_TRACE_ID, traceSpanId.get(LogUtils.TCID));
            request.getAttachmentKv().put(Constants.X_B3_SPAN_ID, traceSpanId.get(LogUtils.SPID));
            // 兼容stargate逻辑
            if (request.getAttachmentKv().get(Constants.STARGATE_SESSION_ID_KEY) == null) {
                request.getAttachmentKv().put(Constants.STARGATE_SESSION_ID_KEY, traceSpanId.get(LogUtils.TCID));
            }
            request.getAttachmentKv().computeIfAbsent(Constants.STARGATE_REQUEST_ID_KEY,
                k -> String.valueOf(request.getId()));

            LogUtils.addLogTimeAttachment(request, Constants.BEFORE_CLIENT_REQUEST_TIME_KEY,
                System.currentTimeMillis());
        } catch (Throwable e) {
            LOGGER.warn("ClientMonitorFilter filterRequest failed, cause by ", e);
        }
        invoker.invoke(request, callback);
    }

    @Override
    public void filterResponse(Response response, Request request) {
        try {
            Object beforeRequestTime = request.getNoneAdditionKv().get(Constants.BEFORE_CLIENT_REQUEST_TIME_KEY);
            if (!(beforeRequestTime instanceof Long)) {
                LOGGER.warn("Exception occur when record reqlog, "
                    + "msg: BEFORE_CLIENT_REQUEST_TIME_KEY is null in request logkv");
            } else {
                LogUtils.addLogTimeAttachment(request, Constants.CLIENT_REQUEST_COST,
                    System.currentTimeMillis() - ((Long) beforeRequestTime));
                LogUtils.recordRequestLog(request, response);
            }

            // record statistics
            recordStats(request, response);
        } catch (Throwable ignore) {
            LOGGER.warn("ClientMonitorFilter filterResponse failed, cause by ", ignore);
        }
    }

    // record stats
    private void recordStats(Request request, Response response) {
        if (HeartbeatService.HEART_BEAT_SERVICE_NAME.equals(request.getServiceName())) {
            // 心跳检测接口不参与业务统计信息的记录
            return;
        }
        if (request.getRemoteURI() != null && StarlightStatsManager.getStats(request.getRemoteURI()) != null) {
            LOGGER.debug("ClientMonitorFilter start record stats");
            long startTime = System.currentTimeMillis();
            StarlightStatistics statistics = StarlightStatsManager.getStats(request.getRemoteURI());
            statistics.record(request, response);
            long recordStatsCost = System.currentTimeMillis() - startTime;
            LOGGER.debug("ClientMonitorFilter record stats cost {}", recordStatsCost);
            if (recordStatsCost > 3) {
                LOGGER.info("ClientMonitorFilter record stats cost {}", recordStatsCost);
            }
        }
    }
}
