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
import com.baidu.cloud.starlight.core.rpc.SingleStarlightClient;
import com.baidu.cloud.starlight.core.rpc.DefaultStarlightServer;
import com.baidu.cloud.starlight.api.rpc.callback.Callback;
import com.baidu.cloud.starlight.api.rpc.config.TransportConfig;
import com.baidu.cloud.starlight.core.rpc.proxy.JDKProxyFactory;
import com.baidu.cloud.starlight.core.integrate.model.ExtInfo;
import com.baidu.cloud.starlight.core.integrate.model.User;
import com.baidu.cloud.starlight.core.integrate.service.AsyncUserService;
import com.baidu.cloud.starlight.core.integrate.service.UserService;
import com.baidu.cloud.starlight.core.integrate.service.UserServiceImpl;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by liuruisen on 2020/3/6.
 */
public class StarlightIntegrateTest {

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
        starlightServer.export(UserService.class, new UserServiceImpl());
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
        clientConfig.setProtocol("brpc");

        JDKProxyFactory proxyFactory = new JDKProxyFactory();
        UserService userService = proxyFactory.getProxy(UserService.class, clientConfig, starlightClient);

        User user = userService.getUser(1l);
        System.out.println("Sync getUser: " + user);
        Assert.assertEquals(user.getUserId(), 1l);

        AsyncUserService asyncUserService =
            proxyFactory.getProxy(AsyncUserService.class, clientConfig, starlightClient);
        Future<User> future = asyncUserService.getUserFuture(2l);
        System.out.println("Future getUser: " + future.get());
        Assert.assertEquals(future.get().getUserId(), 2l);

        Callback<User> callback = new Callback<User>() {
            @Override
            public void onResponse(User response) {
                System.out.println("Callback getUser: " + response);
                Assert.assertEquals(response.getUserId(), 3l);
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }
        };
        asyncUserService.getUserCallback(3l, callback);

        // delete user
        userService.deleteUser(1l);

        User user1 = new User();
        user1.setUserId(123123);
        user1.setUserName("User1");
        user1.setBalance(1000.21d);
        List<String> tags = new LinkedList<>();
        tags.add("fgh");
        tags.add("123123");
        user1.setTags(tags);
        List<ExtInfo> extInfos = new LinkedList<>();
        ExtInfo extInfo = new ExtInfo("hobby", "learn");
        extInfos.add(extInfo);
        user1.setExtInfos(extInfos);

        // save user
        Long userId = userService.saveUser(user1);
        System.out.println("Save user userId: " + userId);
        Assert.assertTrue(userId == 123123l);

        User user2 = userService.updateUser(user1);
        System.out.println("Update user: " + user2);
        Assert.assertTrue(user2.getUserName().equals("User1"));

        starlightClient.destroy();
        starlightServer.destroy();
    }
}
