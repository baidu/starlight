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
import com.baidu.cloud.starlight.api.exception.StarlightRpcException;
import com.baidu.cloud.starlight.api.rpc.threadpool.ThreadPoolFactory;
import com.baidu.cloud.starlight.core.integrate.model.User;
import com.baidu.cloud.starlight.core.integrate.service.UserService;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.model.RpcResponse;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.core.rpc.threadpool.RpcThreadPoolFactory;
import com.baidu.cloud.starlight.api.transport.channel.RpcChannel;
import io.netty.util.Timeout;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doReturn;

/**
 * Created by liuruisen on 2020/3/23.
 */
public class ClientProcessorTest {

    private ClientProcessor clientProcessor;

    @Before
    public void before() {
        ThreadPoolFactory poolFactory = new RpcThreadPoolFactory();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.MAX_BIZ_WORKER_NUM_KEY, "100");
        URI uri = new URI("protocol", "username", "password", "host", 0, "path", parameters);
        poolFactory.initDefaultThreadPool(uri, "test");
        clientProcessor = new ClientProcessor(poolFactory);
    }

    @Test
    public void getRegistry() {
        try {
            clientProcessor.getRegistry();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof StarlightRpcException);
        }
    }

    @Test
    public void process() throws InterruptedException {
        Response response = new RpcResponse();
        response.setStatus(Constants.SUCCESS_CODE);
        response.setResult(new User());

        RpcCallback rpcCallback = new RpcCallback() {
            @Override
            public void addTimeout(Timeout timeout) {}

            @Override
            public Request getRequest() {
                Request request = new RpcRequest();
                request.setMethodName("getUser");
                request.setServiceClass(UserService.class);
                request.setServiceConfig(new ServiceConfig());
                request.setProtocolName("brpc");
                request.setParams(new Object[] {1l});
                request.setParamsTypes(new Class[] {Long.class});
                request.setReturnType(User.class);
                return request;
            }

            @Override
            public void onResponse(Response response) {
                Assert.assertTrue(response.getStatus() == Constants.SUCCESS_CODE);
            }

            @Override
            public void onError(Throwable e) {

            }
        };
        RpcChannel rpcChannel = Mockito.mock(RpcChannel.class);
        doReturn(rpcCallback).when(rpcChannel).removeCallback(anyLong());
        clientProcessor.process(response, rpcChannel);
        TimeUnit.SECONDS.sleep(1);
        Assert.assertTrue(clientProcessor.completeCount(UserService.class.getName()) >= 1);
    }

    @Test
    public void processError() throws InterruptedException {

        Response response = new RpcResponse();
        response.setStatus(Constants.SUCCESS_CODE);
        response.setResult(new User());

        RpcCallback rpcCallback = new RpcCallback() {
            @Override
            public void addTimeout(Timeout timeout) {}

            @Override
            public Request getRequest() {
                Request request = new RpcRequest();
                request.setMethodName("getUser");
                request.setServiceClass(UserService.class);
                request.setServiceConfig(new ServiceConfig());
                request.setProtocolName("brpc");
                request.setParams(new Object[] {1l});
                request.setParamsTypes(new Class[] {Long.class});
                // request.setBodyType(User.class);
                return request;
            }

            @Override
            public void onResponse(Response response) {
                Assert.assertTrue(response.getStatus() == Constants.SUCCESS_CODE);
            }

            @Override
            public void onError(Throwable e) {
                Assert.assertTrue(e instanceof StarlightRpcException);
            }
        };
        RpcChannel rpcChannel = Mockito.mock(RpcChannel.class);
        doReturn(rpcCallback).when(rpcChannel).removeCallback(anyLong());
        clientProcessor.process(response, rpcChannel);

        TimeUnit.SECONDS.sleep(1);

        try {
            clientProcessor.process(new RpcRequest(), rpcChannel);
        } catch (Exception e) {
            Assert.assertTrue(e instanceof StarlightRpcException);
        }
        clientProcessor.close();
    }

    @Test
    public void waitTaskCount() {
        Assert.assertTrue(clientProcessor.waitTaskCount(UserService.class.getName()) == 0);
    }

    @Test
    public void processingCount() {
        Assert.assertTrue(clientProcessor.processingCount(UserService.class.getName()) == 0);
    }

    @Test
    public void allWaitTaskCount() {
        Assert.assertEquals(Integer.valueOf(0), clientProcessor.allWaitTaskCount());
    }
}