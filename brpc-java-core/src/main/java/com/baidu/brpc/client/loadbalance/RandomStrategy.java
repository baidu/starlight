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

package com.baidu.brpc.client.loadbalance;

import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.channel.BrpcChannel;

/**
 * Simple random select load balance strategy implementation
 */
public class RandomStrategy implements LoadBalanceStrategy {

    private final Random random = new Random();

    @Override
    public void init(RpcClient rpcClient) {

    }

    @Override
    public BrpcChannel selectInstance(CopyOnWriteArrayList<BrpcChannel> instances) {
        long instanceNum = instances.size();
        if (instanceNum == 0) {
            return null;
        }

        int index = (int) (getRandomLong() % instanceNum);
        BrpcChannel brpcChannel = instances.get(index);
        return brpcChannel;
    }

    @Override
    public void destroy() {
    }

    private long getRandomLong() {
        long randomIndex = random.nextLong();
        if (randomIndex < 0) {
            randomIndex = 0 - randomIndex;
        }
        return randomIndex;
    }
}
