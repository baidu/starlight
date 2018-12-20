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

package com.baidu.brpc.server.currentlimit;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.baidu.brpc.protocol.RpcRequest;
import com.baidu.brpc.utils.CustomThreadFactory;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import lombok.extern.slf4j.Slf4j;

/**
 * a simple counter implement of current limit algorithm
 *
 * @author wangjiayin@baidu.com
 * @since 2018/11/26
 */
@Slf4j
public class CounterCurrentLimiter implements CurrentLimiter {

    private final int timeIntervalMs = 200;

    private final int maxReqPerInterval;

    private AtomicInteger count = new AtomicInteger(0);

    private Timer timer = new HashedWheelTimer(new CustomThreadFactory("counterLimiter-timer-thread"));

    /**
     * constructor
     *
     * @param maxQps max query per second
     */
    public CounterCurrentLimiter(int maxQps) {
        if (maxQps <= 0) {
            throw new IllegalArgumentException("maxQps must be positive!");
        }
        this.maxReqPerInterval = maxQps / (1000 / timeIntervalMs);
        timer.newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) {
                count.set(0);
                timer.newTimeout(this, timeIntervalMs, TimeUnit.MILLISECONDS);
            }
        }, timeIntervalMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean isAllowable(RpcRequest request) {
        return count.getAndIncrement() <= maxReqPerInterval;
    }

}
