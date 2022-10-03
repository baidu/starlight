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
 
package com.baidu.cloud.demo.provider;

import com.baidu.cloud.demo.api.UserService;
import com.baidu.cloud.demo.provider.service.UserServiceImpl;
import com.baidu.cloud.starlight.api.rpc.StarlightServer;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.api.rpc.config.TransportConfig;
import com.baidu.cloud.starlight.core.rpc.DefaultStarlightServer;

public class DemoProviderApp {

    public static void main(String[] args) {
        // 初始化server
        TransportConfig transportConfig = new TransportConfig();
        StarlightServer starlightServer = new DefaultStarlightServer("localhost", 8005, transportConfig);
        starlightServer.init();

        // 暴露接口信息
        ServiceConfig serviceConfig = new ServiceConfig();
        starlightServer.export(UserService.class, new UserServiceImpl(), serviceConfig);

        // 开启服务
        starlightServer.serve();

        synchronized (DemoProviderApp.class) {
            try {
                DemoProviderApp.class.wait();
            } catch (Throwable e) {
            }
        }
    }
}