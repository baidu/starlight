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
 
package com.baidu.cloud.starlight.api.rpc.threadpool;

import com.baidu.cloud.starlight.api.rpc.Processor;
import com.baidu.cloud.starlight.api.rpc.RpcService;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Thread pool factory for getting or creating Threadpool. Used by {@link Processor} Created by liuruisen on 2019/12/17.
 */
public interface ThreadPoolFactory {

    /**
     * Get or create ThreadPool for service(interface). If no thread pool is specified, the default thread pool is
     * returned
     * 
     * @param rpcService
     * @return
     */
    ThreadPoolExecutor getThreadPool(RpcService rpcService);

    /**
     * Get default Thread pool
     * 
     * @return
     */
    ThreadPoolExecutor getThreadPool();

    /**
     * Close the thread pool factory Resource clear，clear the task and close the thread pool
     */
    void close();

}
