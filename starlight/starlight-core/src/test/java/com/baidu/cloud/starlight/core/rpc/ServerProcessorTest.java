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

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.common.URI;
import com.baidu.cloud.starlight.api.rpc.RpcService;
import com.baidu.cloud.starlight.api.rpc.ServiceInvoker;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.api.exception.StarlightRpcException;
import com.baidu.cloud.starlight.api.rpc.threadpool.ThreadPoolFactory;
import com.baidu.cloud.starlight.core.integrate.service.UserService;
import com.baidu.cloud.starlight.core.integrate.service.UserServiceImpl;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.model.RpcResponse;
import com.baidu.cloud.starlight.core.rpc.threadpool.RpcThreadPoolFactory;
import com.baidu.cloud.starlight.api.transport.channel.RpcChannel;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;

/**
 * Created by liuruisen on 2020/3/23.
 */
public class ServerProcessorTest {

    private ServerProcessor serverProcessor;

    @Before
    public void before() {
        RpcServiceRegistry registry = RpcServiceRegistry.getInstance();

        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setInvokeTimeoutMills(1000);
        RpcService rpcService = new RpcService(UserService.class, new UserServiceImpl(), serviceConfig);

        ServiceInvoker invoker = registry.discover(rpcService.getServiceName());
        if (invoker == null) {
            invoker = new RpcServiceInvoker(rpcService);
            registry.register(invoker);
        }

        ThreadPoolFactory poolFactory = new RpcThreadPoolFactory();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.MAX_BIZ_WORKER_NUM_KEY, "100");
        URI uri = new URI("protocol", "username", "password", "host", 0, "path", parameters);
        poolFactory.initDefaultThreadPool(uri, "test");
        serverProcessor = new ServerProcessor(registry, poolFactory);
    }

    @Test
    public void getRegistry() {
        Assert.assertEquals(serverProcessor.getRegistry().rpcServices().size(), 1);
    }

    @Test
    public void process() throws InterruptedException {
        Request request = new RpcRequest();
        request.setMethodName("getUser");
        request.setServiceName(UserService.class.getName());
        request.setProtocolName("brpc");
        request.setParams(new Object[] {1l});

        RpcChannel rpcChannel = Mockito.mock(RpcChannel.class);
        doNothing().when(rpcChannel).send(any());
        serverProcessor.process(request, rpcChannel);

        TimeUnit.SECONDS.sleep(2);

        Assert.assertTrue(serverProcessor.completeCount(request.getServiceName()) > 0);
    }

    @Test
    public void processError() throws InterruptedException {
        Request request = new RpcRequest();
        request.setMethodName("getUser1");
        request.setServiceName(UserService.class.getName());
        request.setProtocolName("brpc");
        request.setParams(new Object[] {1l});

        RpcChannel rpcChannel = Mockito.mock(RpcChannel.class);
        doNothing().when(rpcChannel).send(any());
        // no such method
        serverProcessor.process(request, rpcChannel);

        TimeUnit.SECONDS.sleep(2);
        Assert.assertTrue(serverProcessor.completeCount(request.getServiceName()) > 0);

        RpcServiceRegistry registry = RpcServiceRegistry.getInstance();
        ServiceInvoker serviceInvoker = registry.discover(UserService.class.getName());
        RpcService rpcService = serviceInvoker.getRpcService();
        rpcService.getServiceConfig().setInvokeTimeoutMills(1);

        Request request2 = new RpcRequest();
        request2.setMethodName("deleteUser");
        request2.setServiceName(UserService.class.getName());
        request2.setProtocolName("brpc");
        request2.setParams(new Object[] {1l});

        // timeout
        serverProcessor.process(request2, rpcChannel);

        TimeUnit.SECONDS.sleep(5);
        Assert.assertTrue(serverProcessor.completeCount(request2.getServiceName()) > 0);

        // process null
        try {
            serverProcessor.process(new RpcResponse(), rpcChannel);
        } catch (Exception e) {
            Assert.assertTrue(e instanceof StarlightRpcException);
        }
    }

    @Test
    public void close() {
        serverProcessor.close();
    }

    @Test
    public void waitTaskCount() {
        Assert.assertEquals(0, (int) serverProcessor.waitTaskCount(UserService.class.getName()));
    }

    @Test
    public void processingCount() {
        Assert.assertEquals(0, (int) serverProcessor.processingCount(UserService.class.getName()));
    }

    @Test
    public void allWaitTaskCount() {
        Assert.assertEquals(0, (int) serverProcessor.allWaitTaskCount());
    }
}