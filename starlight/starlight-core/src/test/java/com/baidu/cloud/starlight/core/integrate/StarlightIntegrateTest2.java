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
 
package com.baidu.cloud.starlight.core.integrate;

import com.baidu.cloud.starlight.api.rpc.StarlightClient;
import com.baidu.cloud.starlight.api.rpc.StarlightServer;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.api.rpc.sse.RpcSseEmitter;
import com.baidu.cloud.starlight.core.integrate.service.UserSseService;
import com.baidu.cloud.starlight.core.integrate.service.UserSseServiceImpl;
import com.baidu.cloud.starlight.core.rpc.SingleStarlightClient;
import com.baidu.cloud.starlight.core.rpc.DefaultStarlightServer;
import com.baidu.cloud.starlight.api.rpc.config.TransportConfig;
import com.baidu.cloud.starlight.core.rpc.proxy.JDKProxyFactory;
import com.baidu.cloud.starlight.core.integrate.model.User;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class StarlightIntegrateTest2 {

    // @Test
    public void startServer() throws Exception {
        // init starlight server
        TransportConfig transportConfig = new TransportConfig();
        transportConfig.setIoThreadNum(8);
        transportConfig.setAcceptThreadNum(1);
        transportConfig.setWriteTimeoutMills(3000);
        transportConfig.setAllIdleTimeout(4000);
        StarlightServer starlightServer = new DefaultStarlightServer("localhost", 8005, transportConfig);
        starlightServer.init();

        // export service
        starlightServer.export(UserSseService.class, new UserSseServiceImpl());
        // serve
        starlightServer.serve();

        Thread.currentThread().join();
    }

    @Test
    public void integrate() throws Exception {
        // init starlight server
        TransportConfig transportConfig = new TransportConfig();
        transportConfig.setIoThreadNum(8);
        transportConfig.setAcceptThreadNum(1);
        transportConfig.setWriteTimeoutMills(3000);
        transportConfig.setAllIdleTimeout(4000);
        StarlightServer starlightServer = new DefaultStarlightServer("localhost", 8005, transportConfig);
        starlightServer.init();

        // export service
        starlightServer.export(UserSseService.class, new UserSseServiceImpl());
        // serve
        starlightServer.serve();

        /** Client **/
        TransportConfig config = new TransportConfig();
        config.setIoThreadNum(8);
        config.setWriteTimeoutMills(3000);
        config.setReadIdleTimeout(5000);
        config.setConnectTimeoutMills(3000);
        config.setReadIdleTimeout(100);
        config.setMaxHeartbeatTimes(3);
        config.setChannelType("pool");
        StarlightClient starlightClient = new SingleStarlightClient("localhost", 8005, config);

        starlightClient.init();

        ServiceConfig clientConfig = new ServiceConfig();
        clientConfig.setProtocol("springrest");

        JDKProxyFactory proxyFactory = new JDKProxyFactory();
        UserSseService userService = proxyFactory.getProxy(UserSseService.class, clientConfig, starlightClient);

        for (int i = 0; i < 20; i++) {
            List<User> userList = new ArrayList<>();
            CountDownLatch countDownLatch = new CountDownLatch(1);
            RpcSseEmitter<User> sseEmitter = userService.getUserList();
            sseEmitter.onData(u -> {
                Assert.assertNotNull(u);
                userList.add(u);
            });
            sseEmitter.onError(t -> {
                t.printStackTrace();
                countDownLatch.countDown();
            });

            sseEmitter.onCompletion(() -> {
                countDownLatch.countDown();
            });
            countDownLatch.await();

            Assert.assertTrue(userList.size() == 5);
        }

        User user = new User();
        user.setUserId(1123);
        user.setUserName("test");
        user = userService.addUser(user);

        Assert.assertNotNull(user);

        starlightClient.destroy();
        starlightServer.destroy();
    }
}
