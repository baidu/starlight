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

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class LoadBalanceManager {
    private static volatile LoadBalanceManager instance;
    private Map<Integer, LoadBalanceFactory> loadBalanceFactoryMap;

    public static LoadBalanceManager getInstance() {
        if (instance == null) {
            synchronized(LoadBalanceManager.class) {
                if (instance == null) {
                    instance = new LoadBalanceManager();
                }
            }
        }
        return instance;
    }

    private LoadBalanceManager() {
        loadBalanceFactoryMap = new HashMap<Integer, LoadBalanceFactory>();
    }

    public void registerLoadBalanceFactory(LoadBalanceFactory factory) {
        Integer loadBalanceType = factory.getLoadBalanceType();
        if (loadBalanceFactoryMap.get(loadBalanceType) != null) {
            throw new RuntimeException("load balance factory already exist:" + loadBalanceType);
        }
        loadBalanceFactoryMap.put(loadBalanceType, factory);
        log.info("register load balance factory:{} success", factory.getClass().getSimpleName());
    }

    public LoadBalanceFactory getLoadBalanceFactory(Integer loadBalanceType) {
        return loadBalanceFactoryMap.get(loadBalanceType);
    }

    public LoadBalanceStrategy createLoadBalance(Integer loadBalanceType) {
        LoadBalanceFactory factory = loadBalanceFactoryMap.get(loadBalanceType);
        if (factory == null) {
            throw new IllegalArgumentException("load balance not exist:" + loadBalanceType);
        }
        return factory.createLoadBalance();
    }
}
