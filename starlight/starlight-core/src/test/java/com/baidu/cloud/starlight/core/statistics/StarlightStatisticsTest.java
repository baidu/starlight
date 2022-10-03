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
 
package com.baidu.cloud.starlight.core.statistics;

import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.model.RpcResponse;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Created by liuruisen on 2021/4/25.
 */
public class StarlightStatisticsTest {

    private static final String TEST = "Test";

    @Test
    public void record() {
        StarlightStatistics statistics = new StarlightStatistics();
        statistics.registerStats(TEST, new FixedTimeWindowStats(1000));
        Response response = new RpcResponse();
        statistics.record(new RpcRequest(), response);
        assertTrue(statistics.discoverStats(TEST) instanceof FixedTimeWindowStats);
        assertEquals(1, ((FixedTimeWindowStats) statistics.discoverStats(TEST)).totalReqCount().intValue());
        assertEquals(0, ((FixedTimeWindowStats) statistics.discoverStats(TEST)).failReqCount().intValue());

        response.setStatus(1004);
        statistics.record(new RpcRequest(), response);
        assertEquals(2, ((FixedTimeWindowStats) statistics.discoverStats(TEST)).totalReqCount().intValue());
        assertEquals(1, ((FixedTimeWindowStats) statistics.discoverStats(TEST)).failReqCount().intValue());

        response.setStatus(3001);
        statistics.record(new RpcRequest(), response);
        assertEquals(3, ((FixedTimeWindowStats) statistics.discoverStats(TEST)).totalReqCount().intValue());
        assertEquals(1, ((FixedTimeWindowStats) statistics.discoverStats(TEST)).failReqCount().intValue());

        response.setStatus(2001);
        statistics.record(new RpcRequest(), response);
        assertEquals(4, ((FixedTimeWindowStats) statistics.discoverStats(TEST)).totalReqCount().intValue());
        assertEquals(2, ((FixedTimeWindowStats) statistics.discoverStats(TEST)).failReqCount().intValue());
    }

    @Test
    public void recordMultiThread() {
        StarlightStatistics statistics = new StarlightStatistics();
        for (int i = 0; i < 500; i++) {
            new Thread(() -> {
                statistics.registerStats(TEST, new FixedTimeWindowStats(100));
                statistics.record(new RpcRequest(), new RpcResponse());
            }).start();
        }

        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            // ignore
        }

        assertTrue(statistics.discoverStats(TEST) instanceof FixedTimeWindowStats);
        assertEquals(500, ((FixedTimeWindowStats) statistics.discoverStats(TEST)).totalReqCount().intValue());
        assertEquals(0, ((FixedTimeWindowStats) statistics.discoverStats(TEST)).failReqCount().intValue());
    }

    @Test
    public void removeStats() {
        StarlightStatistics statistics = new StarlightStatistics();
        statistics.registerStats(TEST, new FixedTimeWindowStats(100));
        assertNotNull(statistics.discoverStats(TEST));
        statistics.removeStats(TEST);
        assertNull(statistics.discoverStats(TEST));
    }
}