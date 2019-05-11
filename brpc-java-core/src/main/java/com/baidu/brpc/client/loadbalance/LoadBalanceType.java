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
package com.baidu.brpc.client.loadbalance;

/**
 * Created by huwenwei on 2017/9/29.
 * please use constants of {@link LoadBalanceStrategy} instead.
 */
@Deprecated
public enum LoadBalanceType {

    RANDOM(0, "RANDOM", new RandomStrategy()),
    ROUND_ROBIN(1, "ROUND_ROBIN", new RoundRobinStrategy()),
    WEIGHT(2, "WEIGHT", new WeightStrategy()),
    FAIR(3, "FAIR", new FairStrategy());

    private int id;
    private String name;
    private LoadBalanceStrategy strategy;

    LoadBalanceType(int id, String name, LoadBalanceStrategy strategy) {
        this.id = id;
        this.name = name;
        this.strategy = strategy;
    }

    public static LoadBalanceType parse(int id) {
        for (LoadBalanceType item : LoadBalanceType.values()) {
            if (item.getId() == id) {
                return item;
            }
        }
        throw new IllegalArgumentException("unknown load balance id");
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LoadBalanceStrategy getStrategy() {
        return strategy;
    }
}
