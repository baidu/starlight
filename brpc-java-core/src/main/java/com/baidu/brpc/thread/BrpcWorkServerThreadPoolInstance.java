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
import com.baidu.brpc.utils.ThreadPool;

/**
 * client & server work thread pool single instance
 */
public class BrpcWorkServerThreadPoolInstance {

    private static volatile ThreadPool workThreadPool;

    private static String name = "brpc-server-work-thread";

    private BrpcWorkServerThreadPoolInstance() {

    }

    /**
     * threadNum only works when thread pool instance create in the first time
     */
    public static ThreadPool getOrCreateInstance(int threadNum) {

        if (workThreadPool == null) {
            synchronized(BrpcWorkServerThreadPoolInstance.class) {
                if (workThreadPool == null) {
                    workThreadPool = new ThreadPool(threadNum,
                            new CustomThreadFactory(name));
                }
            }
        }

        return workThreadPool;
    }

    public static ThreadPool getInstance() {
        return workThreadPool;

    }

    public static String getName() {
        return name;
    }

    public static void setName(String name) {
        BrpcWorkServerThreadPoolInstance.name = name;
    }

}
