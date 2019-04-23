/*
 * Copyright (c) 2018 Baidu, Inc. All Rights Reserved.
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

package com.baidu.brpc.interceptor;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import com.baidu.brpc.protocol.standard.Echo;
import com.baidu.brpc.protocol.standard.EchoService;
import com.baidu.brpc.protocol.standard.EchoServiceImpl;
import com.baidu.brpc.server.RpcServer;

public class InterceptorTest {

    private static RpcServer rpcServer;

    private static boolean[] flags = new boolean[4];

    @BeforeClass
    public static void beforeClass() {
        rpcServer = new RpcServer(8000);
        rpcServer.registerService(new EchoServiceImpl());
        rpcServer.getInterceptors().add(new Interceptor() {
            @Override
            public boolean handleRequest(Request request) {
                System.out.println("server handleRequest1");
                return true;
            }

            @Override
            public void aroundProcess(Request request, Response response, InterceptorChain chain) throws Exception {
                flags[0] = true;
                chain.intercept(request, response);
            }

            @Override
            public void handleResponse(Response response) {
                System.out.println("server handleResponse1");
            }
        });

        rpcServer.getInterceptors().add(new Interceptor() {
            @Override
            public boolean handleRequest(Request request) {
                System.out.println("server handleRequest2");
                return true;
            }

            @Override
            public void aroundProcess(Request request, Response response, InterceptorChain chain) throws Exception {
                flags[1] = true;
                chain.intercept(request, response);
            }

            @Override
            public void handleResponse(Response response) {
                System.out.println("server handleResponse2");
            }
        });
        rpcServer.start();
    }

    @AfterClass
    public static void afterClass() {
        if (rpcServer != null) {
            rpcServer.shutdown();
        }
    }

    @Test
    public void test() {
        for (int i = 0; i < 4; ++i) {
            Assert.assertFalse(flags[i]);
        }
        RpcClient rpcClient = new RpcClient("list://127.0.0.1:8000");
        rpcClient.getInterceptors().add(new Interceptor() {
            @Override
            public boolean handleRequest(Request request) {
                System.out.println("client handleRequest1");
                return true;
            }

            @Override
            public void aroundProcess(Request request, Response response, InterceptorChain chain) throws Exception {
                flags[2] = true;
                chain.intercept(request, response);
            }

            @Override
            public void handleResponse(Response response) {
                System.out.println("client handleResponse1");
            }
        });

        rpcClient.getInterceptors().add(new Interceptor() {
            @Override
            public boolean handleRequest(Request request) {
                System.out.println("client handleRequest2");
                return true;
            }

            @Override
            public void aroundProcess(Request request, Response response, InterceptorChain chain) throws Exception {
                flags[3] = true;
                chain.intercept(request, response);
            }

            @Override
            public void handleResponse(Response response) {
                System.out.println("client handleResponse2");
            }
        });

        EchoService echoService = BrpcProxy.getProxy(rpcClient, EchoService.class);
        final String message = "hello";
        Echo.EchoRequest request = Echo.EchoRequest.newBuilder().setMessage(message).build();
        Echo.EchoResponse response = echoService.echo(request);
        Assert.assertEquals(response.getMessage(), message);
        for (int i = 0; i < 4; ++i) {
            Assert.assertTrue(flags[i]);
        }
        rpcClient.stop();
    }

}
