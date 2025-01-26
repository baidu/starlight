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
 
package com.baidu.cloud.starlight.springcloud.server;

import com.baidu.cloud.starlight.api.rpc.StarlightServer;
import com.baidu.cloud.starlight.core.rpc.RpcServiceRegistry;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Created by liuruisen on 2020/3/26.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(value = {"spring.cloud.config.enabled=false", "spring.main.web-application-type=none",
    "spring.application.name=provider", "starlight.server.port=8006", "starlight.server.enable=true",
    "spring.cloud.consul.discovery.enabled=false", "spring.cloud.consul.discovery.heartbeat.enabled=false",
    "spring.cloud.consul.discovery.register=false", "spring.cloud.service-registry.enabled=false",
    "spring.cloud.service-registry.auto-registration.enabled=false", "starlight.server.name=rpc-provider",
    "starlight.server.host=0.0.0.0", "spring.cloud.gravity.enabled=false",
    "spring.cloud.gravity.serverUrl=localhost:7777"}, classes = StarlightServerApp.class)
public class StarlightServerAutoConfigurationTest {

    @Autowired
    private StarlightServerAutoConfiguration serverAutoConfiguration;

    @Autowired
    private StarlightServer starlightServer;

    @Test
    public void starlightServer() {
        Assert.assertNotNull(serverAutoConfiguration);
        Assert.assertNotNull(starlightServer);
        RpcServiceRegistry serviceRegistry = RpcServiceRegistry.getInstance();
        Assert.assertEquals(serviceRegistry.rpcServices().size(), 2 + 1); // + 1 HeartBeatService
    }

}