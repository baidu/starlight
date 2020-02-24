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
package com.baidu.brpc.loadbalance;

import com.baidu.brpc.client.CommunicationClient;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.protocol.Request;

import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Simple weight load balance strategy implementation
 * The channelGroup which has less failedNum will have the more probability
 */
public class WeightStrategy implements LoadBalanceStrategy {
    private Random random = new Random(System.currentTimeMillis());

    @Override
    public void init(RpcClient rpcClient) {
    }

    @Override
    public CommunicationClient selectInstance(
            Request request,
            List<CommunicationClient> instances,
            Set<CommunicationClient> selectedInstances) {
        long instanceNum = instances.size();
        if (instanceNum == 0) {
            return null;
        }

        long sum = 0;
        for (CommunicationClient instance : instances) {
            sum += getWeight(instance.getBrpcChannel().getFailedNum());
        }
        long randWeight = random.nextLong() % sum;
        for (CommunicationClient instance : instances) {
            randWeight -= getWeight(instance.getBrpcChannel().getFailedNum());
            if (randWeight <= 0) {
                return instance;
            }
        }

        return null;
    }

    @Override
    public void destroy() {
    }

    private long getWeight(long failedNum) {
        return 1000000000 / (failedNum + 1);
    }
}
