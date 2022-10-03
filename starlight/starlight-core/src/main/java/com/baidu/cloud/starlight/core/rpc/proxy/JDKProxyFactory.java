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
 
package com.baidu.cloud.starlight.core.rpc.proxy;

import com.baidu.cloud.starlight.api.rpc.StarlightClient;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.api.rpc.proxy.ProxyFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * Created by liuruisen on 2020/2/28.
 */
public class JDKProxyFactory implements ProxyFactory {

    @Override
    public <T> T getProxy(Class<T> targetType, ServiceConfig serviceConfig, StarlightClient client) {
        client.refer(targetType, serviceConfig); // first refer target service

        InvocationHandler handler = new JdkInvocationHandler(targetType, serviceConfig, client);
        T proxy = (T) Proxy.newProxyInstance(targetType.getClassLoader(), new Class<?>[] {targetType}, handler);
        return proxy;
    }

}
