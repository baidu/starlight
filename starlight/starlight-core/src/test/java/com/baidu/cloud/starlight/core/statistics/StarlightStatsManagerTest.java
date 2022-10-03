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

import com.baidu.cloud.starlight.api.common.URI;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.model.RpcResponse;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Created by liuruisen on 2021/4/25.
 */
public class StarlightStatsManagerTest {

    @Test
    public void getOrCreateStats() {
        URI uri = new URI("brpc", null, null, "localhost", 8080, null, null);
        StarlightStatsManager.getOrCreateStats(uri);
        assertNotNull(StarlightStatsManager.getStats(uri));
    }

    @Test
    public void getOrCreateStatsMultiThread() {
        URI uri = new URI("brpc", null, null, "localhost", 8080, null, null);

        for (int i = 0; i < 500; i++) {
            new Thread(() -> {
                StarlightStatsManager.getOrCreateStats(uri);
                StarlightStatistics statistics = StarlightStatsManager.getStats(uri);
                statistics.registerStats("TEST", new FixedTimeWindowStats(100));
                statistics.record(new RpcRequest(), new RpcResponse());
            }).start();
        }
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            // ignore
        }
        StarlightStatistics statistics = StarlightStatsManager.getStats(uri);
        assertNotNull(statistics);
        assertTrue(statistics.discoverStats("TEST") instanceof FixedTimeWindowStats);
        assertEquals(500, ((FixedTimeWindowStats) statistics.discoverStats("TEST")).totalReqCount().intValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getOrCreateStatsError() {
        StarlightStatsManager.getOrCreateStats(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getOrCreateStatsErrorEmpty() {
        URI uri = new URI("brpc", null, null, null, -1, null, null);
        StarlightStatsManager.getOrCreateStats(uri);
    }

    @Test
    public void removeStats() {
        URI uri = new URI("brpc", null, null, "localhost", 8080, null, null);
        StarlightStatsManager.getOrCreateStats(uri);
        assertNotNull(StarlightStatsManager.getStats(uri));

        StarlightStatsManager.removeStats(uri);
        assertNull(StarlightStatsManager.getStats(uri));
    }
}