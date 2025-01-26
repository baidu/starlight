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
 
package com.baidu.cloud.starlight.springcloud.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;

import com.baidu.cloud.starlight.springcloud.client.cluster.FailFastClusterClient;
import com.baidu.cloud.starlight.springcloud.client.cluster.FailOverClusterClient;
import com.baidu.cloud.starlight.springcloud.client.cluster.SingleStarlightClientManager;

public class StarlightClientLifecycle implements SmartLifecycle {
    private static final Logger LOGGER = LoggerFactory.getLogger(StarlightClientLifecycle.class);

    private volatile boolean running = false;

    private volatile boolean firstStart = true;

    private final SingleStarlightClientManager singleStarlightClientManager;

    private final ApplicationContext applicationContext;

    public StarlightClientLifecycle(SingleStarlightClientManager singleStarlightClientManager,
        ApplicationContext applicationContext) {
        this.singleStarlightClientManager = singleStarlightClientManager;
        this.applicationContext = applicationContext;
    }

    @Override
    public void start() {
        LOGGER.info("StarlightClientLifecycle start: thread: {}, loader:{}", Thread.currentThread(),
            Thread.currentThread().getContextClassLoader());
        // 初始化 Client, ClusterClient 或者 SingleClient
        if (!firstStart) {
            for (String beanName : applicationContext.getBeanNamesForType(FailFastClusterClient.class)) {
                FailFastClusterClient client = (FailFastClusterClient) applicationContext.getBean(beanName);
                client.init();
            }
            for (String beanName : applicationContext.getBeanNamesForType(FailOverClusterClient.class)) {
                FailOverClusterClient client = (FailOverClusterClient) applicationContext.getBean(beanName);
                client.init();
            }
        }

        this.running = true;
        this.firstStart = false;
    }

    @Override
    public void stop(Runnable callback) {
        LOGGER.info("StarlightClientLifecycle stop: thread: {}, loader:{}", Thread.currentThread(),
            Thread.currentThread().getContextClassLoader());

        singleStarlightClientManager.destroyAll();

        this.running = false;
        callback.run();
    }

    @Override
    public void stop() {
        throw new UnsupportedOperationException("Stop must not be invoked directly");
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    /**
     * phase 越小，越早执行 start(), 越晚执行 stop() 设计 Client 的 phase 大于 server
     * 
     * @return
     */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 11;
    }
}
