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
 
package com.baidu.cloud.starlight.springcloud.client.ribbon.lalb;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.statistics.Stats;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * LALB负载均衡场景记录请求时延信息 Created by liuruisen on 2020/10/9.
 */
public class LalbLatencyStats implements Stats {

    /**
     * 计算平均响应时间的窗口大小，太长对于变化感知不及时，太短准确性较低 综合考虑后，采取stargate实践验证过的数值 10
     */
    public static final Integer DEFAULT_WINDOW_SIZE = 10;

    private final Queue<Long> latencyWindow;

    public LalbLatencyStats() {
        this.latencyWindow = new ConcurrentLinkedQueue<>();
    }

    public Queue<Long> getLatencyWindow() {
        return latencyWindow;
    }

    /**
     *
     * @param latency
     */
    public void updateLatencyWindow(Long latency) {
        Queue<Long> latencyWindow = getLatencyWindow();
        latencyWindow.add(latency);
        if (latencyWindow.size() > DEFAULT_WINDOW_SIZE) {
            latencyWindow.poll(); // clear excess data
        }
    }

    /**
     * ServiceInstance avg latency
     *
     * @return
     */
    public Long avgLatency() {
        return avgLatency(latencyWindow.size());
    }

    protected Long avgLatency(int windowSize) {
        if (windowSize == 0) {
            return 0L;
        }
        Long latencies = 0L;
        for (Long latency : latencyWindow) {
            latencies += latency;
        }
        return latencies / windowSize;
    }

    @Override
    public void record(Request request, Response response) {
        Long latency = latency(request, response);
        if (latency != null) {
            updateLatencyWindow(latency);
        }
    }

    private Long latency(Request request, Response response) {
        Object latency = request.getNoneAdditionKv().get(Constants.CLIENT_REQUEST_COST);
        if (latency instanceof Long) {
            return (Long) latency;
        }
        // Impossible to happen
        return null;
    }
}
