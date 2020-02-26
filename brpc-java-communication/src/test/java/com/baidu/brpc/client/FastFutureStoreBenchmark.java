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

package com.baidu.brpc.client;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * benchmark
 *
 * Created by wanghongfei on 2018/11/19.
 */
public class FastFutureStoreBenchmark {
    @Test
    public void testBenchmark() throws Exception {
        // int times = 200000;
        // int times = 1500000;
        int times = 3000000;
        // int times = 6000000;

        // warm jvm
        runMap(times, 1);
        runStore(times, 1);
        runMap(times, 1);
        runStore(times, 1);

        System.out.println("warm done");

        List<Double> all = new ArrayList<Double>();
        for (int ix = 0 ; ix < 50; ++ix) {
            long t1 = runStore(times, 4);

            long t2 = runMap(times, 4);

            System.out.println("diff = " + (t2 - t1) + ", " + ((double) t2) / t1);

            double ratio = ((double) t2 / t1);
            all.add(ratio);
        }

        double sum = 0;
        for (Double n : all) {
            sum += n;
        }

        double result = sum / all.size();
        System.out.println();
        System.out.println(result);

        // Runtime.getRuntime().exec("say -v Ting-ting benchmark完成了, 结果是 " + result);
    }

    /**
     * 运行FastFutureStore benchmark
     *
     * @param count 最大id数量
     * @param threadCount 并发线程数
     * @return
     * @throws Exception
     */
    private long runStore(final int count, final int threadCount) throws Exception {
        final AtomicLong result = new AtomicLong(0);

        final FastFutureStore store = new FastFutureStore(count);

        List<Thread> thList = new ArrayList<Thread>(threadCount);
        for (int c = 0; c < threadCount; ++c) {
            Thread th = new Thread(new Runnable() {
                @Override
                public void run() {

                    long t1 = System.currentTimeMillis();

                    List<Long> idList = new ArrayList<Long>(count / threadCount);
                    for (int ix = 0; ix < count / threadCount; ++ix) {
                        long id = store.put(new RpcFuture());
                        idList.add(id);
                    }


                    for (int ix = 0; ix < count / threadCount; ++ix) {
                        long id = idList.get(ix);
                        RpcFuture fut = store.getAndRemove(id);
                        if (null == fut) {
                            throw new IllegalStateException("fut is null");
                        }
                    }

                    long t2 = System.currentTimeMillis();

                    result.addAndGet(t2 - t1);
                }
            });

            thList.add(th);
            th.start();
        }

        for (Thread th : thList) {
            th.join();
        }

        return result.get();
    }

    /**
     * 运行ConcurrentHashMap benchmark
     * @param count
     * @param threadCount
     * @return
     * @throws Exception
     */
    private long runMap(final int count, final int threadCount) throws Exception {
        final AtomicLong result = new AtomicLong(0);

        // 为了防止Hashmap扩容, 将每条线程需要执行的操作次数 / 0.75的结果作为map初始容量
        int singleThreadCount = count / threadCount;
        int cap = (int) (singleThreadCount / 0.75);
        final Map<Long, RpcFuture> map = new ConcurrentHashMap<Long, RpcFuture>(cap);
        // final Map<Long, RpcFuture> map = new HashMap<Long, RpcFuture>(cap);

        final AtomicLong idGen = new AtomicLong(0);

        List<Thread> thList = new ArrayList<Thread>(threadCount);
        for (int c = 0; c < threadCount; ++c) {
            Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                    long t1 = System.currentTimeMillis();

                    List<Long> idList = new ArrayList<Long>(count / threadCount);
                    for (int ix = 0; ix < count / threadCount; ++ix) {
                        long id = idGen.getAndIncrement();
                        idList.add(id);
                        map.put(id, new RpcFuture());
                    }


                    for (int ix = 0; ix < count / threadCount; ++ix) {
                        long id = idList.get(ix);
                        RpcFuture fut = map.remove(id);
                        if (null == fut) {
                            throw new IllegalStateException("fut is null");
                        }
                    }

                    long t2 = System.currentTimeMillis();

                    result.addAndGet(t2 - t1);
                }
            });

            thList.add(th);
            th.start();
        }

        for (Thread th : thList) {
            th.join();
        }

        return result.get();
    }
}
