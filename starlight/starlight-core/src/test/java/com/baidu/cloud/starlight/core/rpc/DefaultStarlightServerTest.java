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

import com.baidu.cloud.starlight.api.heartbeat.HeartbeatService;
import com.baidu.cloud.starlight.api.rpc.RpcService;
import com.baidu.cloud.starlight.api.rpc.ServiceRegistry;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.api.rpc.config.TransportConfig;
import com.baidu.cloud.starlight.core.integrate.service.AsyncUserService;
import com.baidu.cloud.starlight.core.integrate.service.UserService;
import com.baidu.cloud.starlight.core.integrate.service.UserServiceImpl;
import com.baidu.cloud.starlight.api.transport.ServerPeer;
import com.baidu.cloud.starlight.transport.netty.NettyServer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

/**
 * Created by liuruisen on 2020/3/23.
 */
public class DefaultStarlightServerTest {

    @Test
    public void init() throws NoSuchFieldException, IllegalAccessException {
        DefaultStarlightServer starlightServer =
            new DefaultStarlightServer("brpc", "localhost", 8005, new TransportConfig());
        starlightServer.init();
        starlightServer.unexport(HeartbeatService.class); // 排除并发单测时的干扰
        Field field = starlightServer.getClass().getDeclaredField("serverPeer");
        field.setAccessible(true);
        ServerPeer serverPeer = (ServerPeer) field.get(starlightServer);
        Assert.assertNotNull(serverPeer);
        Assert.assertTrue(serverPeer instanceof NettyServer);
        starlightServer.destroy();
    }

    @Test
    public void serve() throws NoSuchFieldException, IllegalAccessException, InterruptedException {
        DefaultStarlightServer starlightServer =
            new DefaultStarlightServer("brpc", "localhost", 8005, new TransportConfig());
        starlightServer.init();
        starlightServer.unexport(HeartbeatService.class); // 排除并发单测时的干扰
        starlightServer.serve();
        Field field = starlightServer.getClass().getDeclaredField("serverPeer");
        field.setAccessible(true);
        ServerPeer serverPeer = (ServerPeer) field.get(starlightServer);
        Assert.assertNotNull(serverPeer);
        Assert.assertTrue(serverPeer instanceof NettyServer);
        Assert.assertTrue(serverPeer.isBound());

        starlightServer.destroy();
        TimeUnit.SECONDS.sleep(2);
        Assert.assertFalse(serverPeer.isBound());
    }

    @Test
    public void export() {
        DefaultStarlightServer starlightServer =
            new DefaultStarlightServer("brpc", "localhost", 8005, new TransportConfig());
        starlightServer.init();
        starlightServer.unexport(HeartbeatService.class); // 排除并发单测时的干扰
        starlightServer.unexport(UserService.class); // 排除并发单测时的干扰
        starlightServer.export(UserService.class, new UserServiceImpl());
        RpcService rpcService = new RpcService(UserService.class, new UserServiceImpl());
        Assert.assertNotNull(RpcServiceRegistry.getInstance().discover(rpcService.getServiceName()));
        starlightServer.destroy();
    }

    @Test
    public void unexport() {
        DefaultStarlightServer starlightServer =
            new DefaultStarlightServer("brpc", "localhost", 8005, new TransportConfig());
        starlightServer.init();
        starlightServer.unexport(HeartbeatService.class); // 排除并发单测时的干扰
        starlightServer.unexport(AsyncUserService.class); // 排除并发单测时的干扰
        starlightServer.export(AsyncUserService.class, new UserServiceImpl());
        RpcService rpcService = new RpcService(AsyncUserService.class, new UserServiceImpl());
        Assert.assertNotNull(RpcServiceRegistry.getInstance().discover(rpcService.getServiceName()));
        starlightServer.unexport(rpcService);
        Assert.assertNull(RpcServiceRegistry.getInstance().discover(rpcService.getServiceName()));
        starlightServer.destroy();
    }
}