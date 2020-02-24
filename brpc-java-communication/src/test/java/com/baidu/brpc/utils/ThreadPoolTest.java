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

package com.baidu.brpc.utils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolTest {
    @Before
    public void setup() {

    }

    @After
    public void teardown() {

    }

    @Test
    public void sanity() {
        ThreadPool threadPool = new ThreadPool(2, new CustomThreadFactory("test"));
        for (int i = 0; i < 100; ++i) {
            threadPool.submit(new Runnable() {
                @Override
                public void run() {
                    System.out.println("I was run");
                }
            });
        }
        System.out.println("here");
        threadPool.stop();
        threadPool.join();
    }

    @Test
    public void fifo() {
        // Only one consumer thread
        ThreadPool threadPool = new ThreadPool(1, new CustomThreadFactory("test"));
        final AtomicInteger numConsumed = new AtomicInteger(0);
        final AtomicInteger numCreated = new AtomicInteger(0);
        for (int i = 0; i < 100; ++i) {
            threadPool.submit(new Runnable() {
                int index = numCreated.getAndAdd(1);

                @Override
                public void run() {
                    Assert.assertEquals(numConsumed.getAndAdd(1), index);
                }
            });
        }
        threadPool.stop();
        threadPool.join();
    }

}
