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

import com.baidu.cloud.starlight.api.rpc.RpcService;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.api.rpc.config.TransportConfig;
import com.baidu.cloud.starlight.core.integrate.service.UserService;
import com.baidu.cloud.starlight.core.integrate.service.UserServiceImpl;
import com.baidu.cloud.starlight.api.transport.ServerPeer;
import com.baidu.cloud.starlight.transport.netty.NettyServer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

/**
 * Created by liuruisen on 2020/3/23.
 */
public class DefaultStarlightServerTest {

    private DefaultStarlightServer starlightServer;
    private TransportConfig transportConfig;

    @Before
    public void before() {
        transportConfig = new TransportConfig();
        transportConfig.setAcceptThreadNum(1);
        transportConfig.setIoThreadNum(2);
        transportConfig.setAllIdleTimeout(100);
        transportConfig.setConnectTimeoutMills(1000);
        transportConfig.setWriteTimeoutMills(1000);
        starlightServer = new DefaultStarlightServer("brpc", "localhost", 8001, transportConfig);

    }

    @Ignore
    @Test
    public void init() throws NoSuchFieldException, IllegalAccessException {

        starlightServer.init();
        Field field = starlightServer.getClass().getDeclaredField("serverPeer");
        field.setAccessible(true);
        ServerPeer serverPeer = (ServerPeer) field.get(starlightServer);
        Assert.assertNotNull(serverPeer);
        Assert.assertTrue(serverPeer instanceof NettyServer);
        starlightServer.destroy();
    }

    @Ignore
    @Test
    public void serve() throws NoSuchFieldException, IllegalAccessException, InterruptedException {
        starlightServer.init();
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
        starlightServer.init();
        starlightServer.export(UserService.class, new UserServiceImpl());
        RpcService rpcService = new RpcService(UserService.class, new UserServiceImpl());
        Assert.assertNotNull(RpcServiceRegistry.getInstance().discover(rpcService.getServiceName()));
        starlightServer.destroy();
    }

    @Ignore
    @Test
    public void testExport() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setThreadPoolSize(1);
        starlightServer.init();
        starlightServer.export(UserService.class, new UserServiceImpl(), serviceConfig);
        RpcService rpcService = new RpcService(UserService.class, new UserServiceImpl(), serviceConfig);
        Assert.assertNotNull(RpcServiceRegistry.getInstance().discover(rpcService.getServiceName()));
    }

    @Ignore
    @Test
    public void unexport() {
        starlightServer.export(UserService.class, new UserServiceImpl());
        RpcService rpcService = new RpcService(UserService.class, new UserServiceImpl());
        Assert.assertNotNull(RpcServiceRegistry.getInstance().discover(rpcService.getServiceName()));
        starlightServer.unexport(rpcService);
        Assert.assertNull(RpcServiceRegistry.getInstance().discover(rpcService.getServiceName()));
    }
}