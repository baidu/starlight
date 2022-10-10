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
import com.baidu.cloud.starlight.api.rpc.RpcService;
import com.baidu.cloud.starlight.api.rpc.threadpool.NamedThreadFactory;
import com.baidu.cloud.starlight.api.rpc.threadpool.ThreadPoolFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by liuruisen on 2020/2/13.
 */
public class RpcThreadPoolFactory implements ThreadPoolFactory {

    private final ThreadPoolExecutor defaultThreadPool;

    public RpcThreadPoolFactory(int defaultSize, int maxSize, String prefix) {

        // DEFAULT_BIZ_THREAD_POOL_SIZE
        // DEFAULT_MAX_BIZ_THREAD_POOL_SIZE
        defaultThreadPool = new ThreadPoolExecutor(defaultSize, maxSize, Constants.IDlE_THREAD_KEEP_ALIVE_SECOND,
            TimeUnit.SECONDS, new SynchronousQueue<>(), new NamedThreadFactory(prefix + "-biz-work"));
    }
    
    public RpcThreadPoolFactory(ThreadPoolExecutor threadPool) {
        this.defaultThreadPool = threadPool;
    }
    
    private final Map<RpcService, ThreadPoolExecutor> threadPoolMap = new ConcurrentHashMap<>();

    @Override
    public ThreadPoolExecutor getThreadPool(RpcService rpcService) {

        // default Thread pool
        if (rpcService == null) {
            return defaultThreadPool;
        }

        if (threadPoolMap.get(rpcService) != null) {
            return threadPoolMap.get(rpcService);
        }

        if (rpcService.getServiceConfig() != null) {
            ThreadPoolExecutor threadPool = rpcService.getServiceConfig().getThreadPool();
            Integer corePoolSize = rpcService.getServiceConfig().getThreadPoolSize();
            Integer maxThreadPoolSize = rpcService.getServiceConfig().getMaxThreadPoolSize();
            Integer keepAliveTime = rpcService.getServiceConfig().getIdleThreadKeepAliveSecond();
            Integer maxQueueSize = rpcService.getServiceConfig().getMaxRunnableQueueSize();

            if (corePoolSize != null && maxThreadPoolSize != null && keepAliveTime != null && maxQueueSize != null
            || threadPool != null) {
                synchronized (this) {
                    if (threadPoolMap.get(rpcService) != null) {
                        return threadPoolMap.get(rpcService);
                    }
                    threadPool = Optional.ofNullable(threadPool).orElseGet(() -> new ThreadPoolExecutor(
                            corePoolSize, maxThreadPoolSize, keepAliveTime, TimeUnit.SECONDS,
                            new LinkedBlockingQueue<>(maxQueueSize),
                            new NamedThreadFactory("service-biz-work")));
                    threadPoolMap.put(rpcService, threadPool);
                }
                return threadPool;
            } else {
                return defaultThreadPool;
            }

        } else { // No own thread pool configuration, shared the default thread pool
            return defaultThreadPool;
        }
    }

    @Override
    public ThreadPoolExecutor getThreadPool() {
        return defaultThreadPool;
    }

    @Override
    public void close() {
        if (threadPoolMap.size() == 0) {
            return;
        }
        for (ThreadPoolExecutor threadPool : threadPoolMap.values()) {
            if (!threadPool.isShutdown()) {
                threadPool.shutdown(); // shutdown now
            }
        }
        threadPoolMap.clear();
        // fixme default thread pool何时关闭？不手动关闭会有啥影响
    }

}
