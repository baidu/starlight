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
 
package com.baidu.cloud.starlight.core.rpc.threadpool;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.common.URI;
import com.baidu.cloud.starlight.api.rpc.RpcService;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.api.rpc.threadpool.NamedThreadFactory;
import com.baidu.cloud.starlight.api.rpc.threadpool.ThreadPoolFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by liuruisen on 2020/2/13.
 */
public class RpcThreadPoolFactory implements ThreadPoolFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcThreadPoolFactory.class);

    private ThreadPoolExecutor defaultThreadPool;

    /**
     * for server side
     */
    private final Map<RpcService, ThreadPoolExecutor> threadPoolMap = new ConcurrentHashMap<>();

    @Override
    public void initDefaultThreadPool(URI uri, String threadPrefix) {
        // default max=500
        int maxPoolSize =
            uri.getParameter(Constants.MAX_BIZ_WORKER_NUM_KEY, Constants.DEFAULT_MAX_BIZ_THREAD_POOL_SIZE);
        defaultThreadPool = new ThreadPoolExecutor(Constants.DEFAULT_BIZ_THREAD_POOL_SIZE, maxPoolSize,
            Constants.IDlE_THREAD_KEEP_ALIVE_SECOND, TimeUnit.SECONDS, new SynchronousQueue<>(),
            new NamedThreadFactory(threadPrefix));
    }

    @Override
    public ThreadPoolExecutor getThreadPool(RpcService rpcService) {

        // default Thread pool
        if (rpcService == null) {
            return defaultThreadPool;
        }

        if (threadPoolMap.get(rpcService) != null) {
            return threadPoolMap.get(rpcService);
        }

        ServiceConfig serviceConfig = rpcService.getServiceConfig();
        if (serviceConfig == null) {
            return defaultThreadPool;
        }

        if (serviceConfig.getCustomizeThreadPool() == null || !(serviceConfig.getCustomizeThreadPool())) {
            return defaultThreadPool;
        }

        Integer corePoolSize = serviceConfig.getThreadPoolSize();
        Integer maxThreadPoolSize = serviceConfig.getMaxThreadPoolSize();
        Integer keepAliveTime = serviceConfig.getIdleThreadKeepAliveSecond();
        Integer maxQueueSize = serviceConfig.getMaxRunnableQueueSize();

        try {
            ThreadPoolExecutor threadPool;
            synchronized (this) {
                if (threadPoolMap.get(rpcService) != null) {
                    return threadPoolMap.get(rpcService);
                }
                threadPool = new ThreadPoolExecutor(corePoolSize, maxThreadPoolSize, keepAliveTime, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(maxQueueSize), new NamedThreadFactory("service-biz-work"));
                threadPoolMap.put(rpcService, threadPool);
            }
            return threadPool;
        } catch (Exception e) {
            LOGGER.warn("Create service thread pool failed, will use default thread pool");
            return defaultThreadPool;
        }
    }

    @Override
    public ThreadPoolExecutor defaultThreadPool() {
        return defaultThreadPool;
    }

    @Override
    public void close() {
        for (ThreadPoolExecutor threadPool : threadPoolMap.values()) {
            if (!threadPool.isShutdown()) {
                threadPool.shutdown(); // shutdown now
            }
        }
        threadPoolMap.clear();
        // default thread pool在客户端场景会所有客户端公用，注意close的时机
        if (defaultThreadPool != null) {
            defaultThreadPool.shutdown();
        }
    }

}
