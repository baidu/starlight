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
 
package com.baidu.cloud.starlight.api.utils;

import org.junit.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Created by liuruisen on 2021/7/30.
 */
public class IDGeneratorTest {

    @Test
    public void genID() throws InterruptedException {
        Map<Long, Long> result = new ConcurrentHashMap<>();

        ExecutorService executorService = Executors.newFixedThreadPool(500);

        for (int i = 0; i < 500; i++) {
            executorService.execute(() -> {
                IDGenerator generator = IDGenerator.getInstance();
                for (int j = 0; j < (1 << 10); j++) {
                    result.put(generator.nextId(), 1L);
                }
            });
        }

        TimeUnit.SECONDS.sleep(30);
        assertEquals(500 * (1 << 10), result.size());
    }

    @Test
    public void testError() {
        Integer intV = 2147483647;
        System.out.println(intV);
        intV = intV << 8;
        System.out.println(intV);
    }

}