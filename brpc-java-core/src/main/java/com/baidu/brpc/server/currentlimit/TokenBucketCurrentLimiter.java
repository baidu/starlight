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
 * Token bucket algorithm
 * Advantage: allow certain burst traffic, and flow control is smoother
 *
 * @author wangjiayin@baidu.com
 * @since 2018/11/26
 */
@Slf4j
public class TokenBucketCurrentLimiter implements CurrentLimiter {

    // the bucket size, namely the max concurrency
    private final int bucketSize;
    // Time interval for put tokens into the bucket
    private final int timeIntervalMs = 200;
    // Number of tokens added to the bucket per time period
    private int tokenPerInterval;
    // current token num in the bucket
    private AtomicInteger currentToken;

    private Timer timer = new HashedWheelTimer(new CustomThreadFactory("tokenBucketLimiter-timer-thread"));

    public TokenBucketCurrentLimiter(int bucketSize, int tokenInputRate) {
        if (bucketSize <= 0 || tokenInputRate <= 0) {
            throw new IllegalArgumentException("bucketSize and rate must be positive!");
        }
        this.bucketSize = bucketSize;
        this.tokenPerInterval = tokenInputRate / (1000 / timeIntervalMs);
        if (this.tokenPerInterval == 0) {
            this.tokenPerInterval = 1;
        }
        this.currentToken = new AtomicInteger(bucketSize);
        timer.newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                supply();
                timer.newTimeout(this, timeIntervalMs, TimeUnit.MILLISECONDS);
            }
        }, timeIntervalMs, TimeUnit.MILLISECONDS);
    }

    /**
     * get a token
     * if there's no more token in the bucket, return immediately
     *
     * @return if success
     */
    private boolean acquire() {
        return currentToken.get() > 0 && currentToken.getAndDecrement() > 0;
    }

    /**
     * put some tokens into the bucket
     */
    private void supply() {
        // 这里就不加锁了
        if (currentToken.getAndAdd(tokenPerInterval) > bucketSize) {
            currentToken.set(bucketSize);
        }
    }

    @Override
    public boolean isAllowable(RpcRequest request) {
        return this.acquire();
    }

}
