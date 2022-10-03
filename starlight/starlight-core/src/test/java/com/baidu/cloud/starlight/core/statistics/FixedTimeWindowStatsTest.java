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

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Created by liuruisen on 2021/4/25.
 */
public class FixedTimeWindowStatsTest {

    @Test
    public void recordReqCount() {
        FixedTimeWindowStats stats = new FixedTimeWindowStats(180);

        for (int i = 0; i < 500; i++) {
            int finalI = i;
            new Thread(() -> {
                if (finalI < 200) {
                    stats.recordReqCount(true);
                } else {
                    stats.recordReqCount(false);
                }
            }).start();
        }

        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            // ignore
        }

        System.out.println("ReqCount: " + stats.totalReqCount());
        System.out.println("FailCount: " + stats.failReqCount());
        System.out.println("SuccCount: " + stats.sucReqCount());
        assertEquals(300, stats.failReqCount().intValue());
        assertEquals(500, stats.totalReqCount().intValue());
        assertEquals(200, stats.sucReqCount().intValue());
    }

    @Test
    public void recordReqCountExpire() {
        FixedTimeWindowStats stats = new FixedTimeWindowStats(5);

        for (int i = 0; i < 500; i++) {
            int finalI = i;
            new Thread(() -> {
                if (finalI < 200) {
                    stats.recordReqCount(true);
                } else {
                    stats.recordReqCount(false);
                }
            }).start();
        }

        try {
            TimeUnit.SECONDS.sleep(7);
        } catch (InterruptedException e) {
            // ignore
        }

        System.out.println("ReqCount: " + stats.totalReqCount());
        System.out.println("FailCount: " + stats.failReqCount());
        System.out.println("SuccCount: " + stats.sucReqCount());
        assertEquals(0, stats.failReqCount().intValue());
        assertEquals(0, stats.totalReqCount().intValue());
        assertEquals(0, stats.sucReqCount().intValue());
    }

}