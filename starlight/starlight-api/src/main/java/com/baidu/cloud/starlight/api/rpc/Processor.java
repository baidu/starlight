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
 
package com.baidu.cloud.starlight.api.rpc;

import com.baidu.cloud.starlight.api.model.MsgBase;
import com.baidu.cloud.starlight.api.rpc.threadpool.ThreadPoolFactory;
import com.baidu.cloud.starlight.api.transport.channel.RpcChannel;

/**
 * Processor is responsible for processing Service call. Current used in server side.代表IO线程执行结束后的异步调用执行 Delegate
 * {@link ThreadPoolFactory} to implement special thread pool strategy. Created by liuruisen on 2019/12/17.
 */
public interface Processor {

    /**
     * ServiceRegistry is responsible for holding ServiceInvoker objects. Get ServiceInvoker from ServiceRegistry.
     * 
     * @return ServiceRegistry
     */
    ServiceRegistry getRegistry();

    /**
     * Process Rpc Msg Async process
     * 
     * @param msgBase
     * @param context RpcChannel context
     */
    void process(MsgBase msgBase, RpcChannel context);

    /**
     * Close and release resources
     */
    void close();

    /**
     * ThreadPoolFactory is used to execute real biz invoke
     * 
     * @return
     */
    void setThreadPoolFactory(ThreadPoolFactory threadPoolFactory);

    /**
     * Statistics: the approximate wait request count of the rpc service.
     * 
     * @param serviceKey
     * @return
     */
    Integer waitTaskCount(String serviceKey);

    /**
     * Statistics: the approximate executing task count of the rpc service
     * 
     * @param serviceKey
     * @return
     */
    Integer processingCount(String serviceKey);

    /**
     * Statistics: the approximate completed task count of the rpc service
     * 
     * @param serviceKey
     * @return
     */
    Long completeCount(String serviceKey);

    /**
     * Statistics: the approximate wait request count of all rpc services.
     * 
     * @return
     */
    Integer allWaitTaskCount();

}
