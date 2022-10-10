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
 
package com.baidu.cloud.demo.consumer;

import com.baidu.cloud.demo.api.model.User;
import com.baidu.cloud.starlight.api.rpc.StarlightClient;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.api.rpc.config.TransportConfig;
import com.baidu.cloud.starlight.core.rpc.SingleStarlightClient;
import com.baidu.cloud.starlight.core.rpc.generic.AsyncGenericService;
import com.baidu.cloud.starlight.core.rpc.proxy.JDKProxyFactory;
import com.baidu.cloud.starlight.core.utils.PojoJsonUtils;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.Future;

public class AsyncGenericConsumerApp {

    public static void main(String[] args) throws Exception {
        // 创建Client
        TransportConfig config = new TransportConfig(); // 传输配置
        StarlightClient starlightClient = new SingleStarlightClient("localhost", 8005, config);
        starlightClient.init();

        // 引用服务
        ServiceConfig clientConfig = new ServiceConfig(); // 服务配置
        clientConfig.setProtocol("brpc");
        clientConfig.setServiceId("com.baidu.cloud.demo.api.UserService");

        // 生成代理
        JDKProxyFactory proxyFactory = new JDKProxyFactory();
        AsyncGenericService userService =
            proxyFactory.getProxy(AsyncGenericService.class, clientConfig, starlightClient);

        // 发起调用
        Future<Object> resultFuture = userService.$invokeFuture("getUser", new Object[] {1L});
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) resultFuture.get();
        User user = (User) PojoJsonUtils.realize(new Object[] {result}, new Type[] {User.class})[0];
        System.out.println(user.toString());

        // 销毁client
        starlightClient.destroy();

        System.exit(0);
    }
}
