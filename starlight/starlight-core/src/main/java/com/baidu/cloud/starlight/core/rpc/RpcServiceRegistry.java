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
 
package com.baidu.cloud.starlight.core.rpc;

import com.baidu.cloud.starlight.api.exception.RpcException;
import com.baidu.cloud.starlight.api.rpc.ServiceInvoker;
import com.baidu.cloud.starlight.api.rpc.ServiceRegistry;
import com.baidu.cloud.starlight.api.utils.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by liuruisen on 2020/2/21.
 */
public class RpcServiceRegistry implements ServiceRegistry {

    private static RpcServiceRegistry serviceRegistry;

    private final Map<String, ServiceInvoker> rpcServiceMap;

    // store ServiceName and serviceClass mapping relationship, such serviceId <-->
    // we can support brpc cross-language call by this way
    private final Map<String, String> serviceInterfaceMap;

    private RpcServiceRegistry() {
        this.rpcServiceMap = new ConcurrentHashMap<>();
        this.serviceInterfaceMap = new ConcurrentHashMap<>();
    }

    public static RpcServiceRegistry getInstance() {
        synchronized (RpcServiceRegistry.class) {
            if (serviceRegistry == null) {
                serviceRegistry = new RpcServiceRegistry();
            }
        }
        return serviceRegistry;
    }

    @Override
    public void register(ServiceInvoker serviceInvoker) {
        // register service
        String serviceClassName = serviceInvoker.getRpcService().getServiceClass().getName();
        if (rpcServiceMap.get(serviceClassName) != null) {
            throw new RpcException("Service repeated register, serviceClass: " + serviceClassName);
        }
        rpcServiceMap.put(serviceClassName, serviceInvoker);

        // store serviceName and serviceClass relation map
        String serviceKey = serviceInvoker.getRpcService().getServiceName();
        if (serviceInterfaceMap.get(serviceKey) != null) {
            throw new RpcException("Service repeated register, serviceName: " + serviceKey);
        }
        serviceInterfaceMap.put(serviceKey, serviceClassName);
    }

    @Override
    public void unRegister(ServiceInvoker serviceInvoker) {
        String serviceName = serviceInvoker.getRpcService().getServiceName();
        serviceInterfaceMap.remove(serviceName);

        String serviceClassName = serviceInvoker.getRpcService().getServiceClass().getName();
        rpcServiceMap.remove(serviceClassName);
    }

    @Override
    public ServiceInvoker discover(String serviceName) {
        ServiceInvoker serviceInvoker = rpcServiceMap.get(serviceName);
        if (serviceInvoker != null) {
            return serviceInvoker;
        }
        // serviceId --> serviceClassName
        String serviceClassName = serviceInterfaceMap.get(serviceName);
        if (StringUtils.isEmpty(serviceClassName)) {
            return null;
        }
        return rpcServiceMap.get(serviceClassName);
    }

    @Override
    public Set<ServiceInvoker> rpcServices() {
        Set<ServiceInvoker> serviceInvokers = new HashSet<>();
        serviceInvokers.addAll(rpcServiceMap.values());
        return serviceInvokers;
    }

    @Override
    public void destroy() {
        if (serviceInterfaceMap.size() > 0) {
            serviceInterfaceMap.clear();
        }

        if (rpcServiceMap.size() > 0) {
            rpcServiceMap.clear();
        }
    }
}
