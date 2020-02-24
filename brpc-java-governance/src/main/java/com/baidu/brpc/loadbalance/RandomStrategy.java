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
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Simple random select load balance strategy implementation
 */
public class RandomStrategy implements LoadBalanceStrategy {
    private final Random random = new Random();

    @Override
    public void init(RpcClient rpcClient) {
    }

    @Override
    public CommunicationClient selectInstance(
            Request request,
            List<CommunicationClient> instances,
            Set<CommunicationClient> selectedInstances) {
        if (CollectionUtils.isEmpty(instances)) {
            return null;
        }

        Collection<CommunicationClient> toBeSelectedInstances = null;
        if (selectedInstances == null) {
            toBeSelectedInstances = instances;
        } else {
            toBeSelectedInstances = CollectionUtils.subtract(instances, selectedInstances);
        }

        int instanceNum = toBeSelectedInstances.size();
        if (instanceNum == 0) {
            toBeSelectedInstances = instances;
            instanceNum = toBeSelectedInstances.size();
        }

        if (instanceNum == 0) {
            return null;
        }
        int index = getRandomInt(instanceNum);
        CommunicationClient serviceInstance = toBeSelectedInstances.toArray(new CommunicationClient[0])[index];
        return serviceInstance;
    }

    @Override
    public void destroy() {
    }

    private int getRandomInt(int bound) {
        int randomIndex = random.nextInt(bound);
        return randomIndex;
    }
}
