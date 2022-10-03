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

import com.baidu.cloud.starlight.api.rpc.ClientInvoker;
import com.baidu.cloud.starlight.api.transport.PeerStatus;
import com.baidu.cloud.starlight.core.rpc.callback.BizWrapCallback;
import com.baidu.cloud.starlight.api.rpc.callback.Callback;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.api.rpc.config.TransportConfig;
import com.baidu.cloud.starlight.api.exception.StarlightRpcException;
import com.baidu.cloud.starlight.core.integrate.service.AsyncUserService;
import com.baidu.cloud.starlight.core.integrate.service.UserService;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.transport.ClientPeer;
import com.baidu.cloud.starlight.transport.netty.NettyClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;

/**
 * Created by liuruisen on 2020/3/23.
 */
public class DefaultStarlightClientTest {

    private SingleStarlightClient starlightClient;

    @Before
    public void before() {
        TransportConfig transportConfig = new TransportConfig();
        transportConfig.setAcceptThreadNum(1);
        transportConfig.setIoThreadNum(2);
        transportConfig.setAllIdleTimeout(100);
        transportConfig.setConnectTimeoutMills(1000);
        transportConfig.setWriteTimeoutMills(1000);
        starlightClient = new SingleStarlightClient("localhost", 8005, transportConfig);
    }

    @Test
    public void init() throws NoSuchFieldException, IllegalAccessException {
        try {
            starlightClient.init();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Field field = starlightClient.getClass().getDeclaredField("clientPeer");
        field.setAccessible(true);
        ClientPeer ClientPeer = (ClientPeer) field.get(starlightClient);
        Assert.assertNotNull(ClientPeer);
        Assert.assertTrue(ClientPeer instanceof NettyClient);
    }

    @Test
    public void refer() throws IllegalAccessException, NoSuchFieldException {
        try {
            starlightClient.init();
        } catch (Exception e) {
            e.printStackTrace();
        }
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setThreadPoolSize(123);
        starlightClient.refer(UserService.class, serviceConfig);

        Field field = starlightClient.getClass().getDeclaredField("clientInvokers");
        field.setAccessible(true);
        Map map = (Map) field.get(starlightClient);
        Assert.assertTrue(map.size() >= 1);

        // refer illegal
        starlightClient.refer(AsyncUserService.class, serviceConfig);
        Assert.assertTrue(map.size() >= 1);

        starlightClient.destroy();
        Assert.assertTrue(map.size() == 0);
    }

    @Test
    public void request() throws NoSuchFieldException, IllegalAccessException {
        try {
            starlightClient.init();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Field field = starlightClient.getClass().getDeclaredField("clientPeer");
        field.setAccessible(true);
        ClientPeer ClientPeer = (ClientPeer) field.get(starlightClient);
        ClientPeer.updateStatus(new PeerStatus(PeerStatus.Status.ACTIVE, System.currentTimeMillis()));

        Map<String, ClientInvoker> clientInvokers = new HashMap<>();
        ClientInvoker clientInvoker = Mockito.mock(ClientInvoker.class);
        doNothing().when(clientInvoker).invoke(any(), any());
        clientInvokers.put(UserService.class.getName(), clientInvoker);

        Request request = new RpcRequest();
        request.setMethodName("getUser");
        request.setServiceClass(UserService.class);
        request.setServiceConfig(new ServiceConfig());
        request.setProtocolName("brpc");
        request.setParams(new Object[] {1l});
        request.setParamsTypes(new Class[] {Long.class});
        starlightClient.request(request, new BizWrapCallback(new Callback() {
            @Override
            public void onResponse(Object response) {}

            @Override
            public void onError(Throwable e) {}
        }, request));
    }

    @Test
    public void requestError() throws NoSuchFieldException, IllegalAccessException {
        try {
            starlightClient.init();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Field field = starlightClient.getClass().getDeclaredField("clientPeer");
        field.setAccessible(true);
        ClientPeer ClientPeer = (ClientPeer) field.get(starlightClient);
        ClientPeer.updateStatus(new PeerStatus(PeerStatus.Status.ACTIVE, System.currentTimeMillis()));

        Request request = new RpcRequest();
        request.setMethodName("getUser");
        request.setServiceClass(UserService.class);
        request.setServiceConfig(new ServiceConfig());
        request.setProtocolName("brpc");
        request.setParams(new Object[] {1l});
        request.setParamsTypes(new Class[] {Long.class});
        starlightClient.request(request, new BizWrapCallback(new Callback() {
            @Override
            public void onResponse(Object response) {}

            @Override
            public void onError(Throwable e) {
                Assert.assertTrue(e instanceof StarlightRpcException);
                Assert.assertTrue(((StarlightRpcException) e).getCode() == StarlightRpcException.BAD_REQUEST);
            }
        }, request));
    }
}