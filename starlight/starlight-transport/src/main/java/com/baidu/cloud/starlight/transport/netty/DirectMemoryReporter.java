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
 
package com.baidu.cloud.starlight.transport.netty;

import com.baidu.cloud.starlight.api.rpc.threadpool.NamedThreadFactory;
import io.netty.util.internal.PlatformDependent;
import org.springframework.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 收集直接内存占用数据 Created by liuruisen on 2021/12/13.
 */
public class DirectMemoryReporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirectMemoryReporter.class);

    private AtomicLong directMemory;

    private ScheduledThreadPoolExecutor schedulePool;

    public DirectMemoryReporter() {
        init();
        report();
    }

    private void init() {
        try {
            Field field = ReflectionUtils.findField(PlatformDependent.class, "DIRECT_MEMORY_COUNTER");
            field.setAccessible(true);
            directMemory = (AtomicLong) field.get(PlatformDependent.class);
        } catch (IllegalAccessException e) {
            LOGGER.warn("Get DIRECT_MEMORY_COUNTER from PlatformDependent failed.", e);
            return;
        }
        schedulePool = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("DirectMem-"));
    }

    public void report() {
        if (schedulePool == null) {
            return;
        }
        schedulePool.scheduleAtFixedRate(() -> {
            try {
                LOGGER.info("netty_direct_memory: {}B", directMemory.get());
                LOGGER.info("netty_direct_memory: {}K", directMemory.get() / 1024);
            } catch (Throwable e) {
                LOGGER.warn("Get DIRECT_MEMORY_COUNTER from directMemory failed.", e);
            }

        }, 0, 1, TimeUnit.MINUTES);
    }

    public void close() {
        if (schedulePool != null) {
            schedulePool.shutdownNow();
        }
    }
}
