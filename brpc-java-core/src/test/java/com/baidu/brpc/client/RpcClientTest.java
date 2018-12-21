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

package com.baidu.brpc.client;

import com.baidu.brpc.protocol.standard.Echo;
import com.baidu.brpc.protocol.standard.EchoService;
import com.baidu.brpc.protocol.standard.EchoServiceImpl;
import com.baidu.brpc.server.RpcServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class RpcClientTest {
    private static RpcServer rpcServer;

    @BeforeClass
    public static void beforeClass() {
        rpcServer = new RpcServer(8000);
        rpcServer.registerService(new EchoServiceImpl());
        rpcServer.start();
    }

    @AfterClass
    public static void afterClass() {
        if (rpcServer != null) {
            rpcServer.shutdown();
        }
    }

    @Test
    public void testRpcClient() {
        RpcClient rpcClient = new RpcClient("127.0.0.1:8000");
        EchoService echoService = BrpcProxy.getProxy(rpcClient, EchoService.class);
        Echo.EchoRequest request = Echo.EchoRequest.newBuilder().setMessage("hello").build();
        Echo.EchoResponse response = echoService.echo(request);
        System.out.println(response.getMessage());
        rpcClient.stop();
    }

    @Test
    public void testRpcClientWithOptions() {
        RpcClient rpcClient = new RpcClient("127.0.0.1:8000", new RpcClientOptions());
        EchoService echoService = BrpcProxy.getProxy(rpcClient, EchoService.class);
        Echo.EchoRequest request = Echo.EchoRequest.newBuilder().setMessage("hello").build();
        Echo.EchoResponse response = echoService.echo(request);
        System.out.println(response.getMessage());
        rpcClient.stop();
    }

}
