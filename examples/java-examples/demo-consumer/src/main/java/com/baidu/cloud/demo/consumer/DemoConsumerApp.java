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

import com.baidu.cloud.demo.api.UserService;
import com.baidu.cloud.demo.api.model.User;
import com.baidu.cloud.starlight.api.rpc.StarlightClient;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.api.rpc.config.TransportConfig;
import com.baidu.cloud.starlight.core.rpc.SingleStarlightClient;
import com.baidu.cloud.starlight.core.rpc.proxy.JDKProxyFactory;

public class DemoConsumerApp {

    public static void main(String[] args) {
        // 创建Client
        TransportConfig config = new TransportConfig(); // 传输配置
        StarlightClient starlightClient = new SingleStarlightClient("localhost", 8005, config);
        starlightClient.init();

        // 引用服务
        ServiceConfig clientConfig = new ServiceConfig(); // 服务配置
        clientConfig.setProtocol("brpc");
        // clientConfig.setSerializeMode("pb2-java");
        // clientConfig.setServiceId("com.baidu.cloud.rpc.benchmarks.service.EchoService");

        // 生成代理
        JDKProxyFactory proxyFactory = new JDKProxyFactory();
        UserService userService = proxyFactory.getProxy(UserService.class, clientConfig, starlightClient);

        // 发起调用
        User user = userService.getUser(1L);
        System.out.println(user.toString());

        // 销毁client
        starlightClient.destroy();

        System.exit(0);
    }
}
