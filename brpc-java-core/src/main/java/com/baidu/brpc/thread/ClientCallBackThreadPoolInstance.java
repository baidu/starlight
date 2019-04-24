/*
 * Copyright (c) 2018 Baidu, Inc. All Rights Reserved.
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

package com.baidu.brpc.thread;

import com.baidu.brpc.utils.CustomThreadFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientCallBackThreadPoolInstance {

    private static volatile ExecutorService callbackThreadPool;

    private ClientCallBackThreadPoolInstance() {

    }

    /**
     * threadNum only works when thread pool instance create in the first time
     */
    public static ExecutorService getOrCreateInstance(int threadNum) {
        if (callbackThreadPool == null) {
            synchronized (ClientCallBackThreadPoolInstance.class) {
                if (callbackThreadPool == null) {
                    callbackThreadPool = Executors.newFixedThreadPool(threadNum,
                            new CustomThreadFactory("invalid-channel-callback-thread"));
                }
            }
        }

        return callbackThreadPool;
    }

    public static ExecutorService getInstance() {
        return callbackThreadPool;
    }

}
