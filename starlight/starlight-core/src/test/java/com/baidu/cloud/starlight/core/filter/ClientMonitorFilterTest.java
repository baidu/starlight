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

import com.baidu.cloud.starlight.api.common.URI;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.model.RpcResponse;
import com.baidu.cloud.starlight.core.statistics.FixedTimeWindowStats;
import com.baidu.cloud.starlight.core.statistics.StarlightStatistics;
import com.baidu.cloud.starlight.core.statistics.StarlightStatsManager;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by liuruisen on 2021/4/25.
 */
public class ClientMonitorFilterTest {

    @Test
    public void filterResponse() {
        RpcRequest rpcRequest = new RpcRequest();
        ClientMonitorFilter monitorFilter = new ClientMonitorFilter();
        monitorFilter.filterResponse(new RpcResponse(), rpcRequest);

        URI.Builder builder = new URI.Builder("brpc", "localhost", 8006);
        URI uri = builder.build();
        rpcRequest.setRemoteURI(uri);

        StarlightStatistics statistics = StarlightStatsManager.getOrCreateStats(uri);
        statistics.registerStats("TEST", new FixedTimeWindowStats(10));
        monitorFilter.filterResponse(new RpcResponse(), rpcRequest);

        assertTrue(statistics.discoverStats("TEST") instanceof FixedTimeWindowStats);
        assertEquals(0, ((FixedTimeWindowStats) statistics.discoverStats("TEST")).totalReqCount().intValue());
    }
}