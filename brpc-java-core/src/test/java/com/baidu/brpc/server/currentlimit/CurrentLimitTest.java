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

package com.baidu.brpc.server.currentlimit;

import com.baidu.brpc.protocol.standard.Echo;
import com.baidu.brpc.protocol.standard.EchoService;
import com.baidu.brpc.protocol.standard.EchoServiceImpl;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.interceptor.CurrentLimitInterceptor;
import com.baidu.brpc.server.RpcServer;

public class CurrentLimitTest {

    private static RpcServer rpcServer1;

    private static RpcServer rpcServer2;

    @BeforeClass
    public static void beforeClass() {
        rpcServer1 = new RpcServer(8000);
        rpcServer1.registerService(new EchoServiceImpl());
        rpcServer1.getInterceptors().add(new CurrentLimitInterceptor(new TokenBucketCurrentLimiter(500, 500)));

        rpcServer2 = new RpcServer(8001);
        rpcServer2.registerService(new EchoServiceImpl());
        rpcServer2.getInterceptors().add(new CurrentLimitInterceptor(new CounterCurrentLimiter(500)));

        rpcServer1.start();
        rpcServer2.start();
    }

    @AfterClass
    public static void afterClass() {
        if (rpcServer1 != null) {
            rpcServer1.shutdown();
        }
        if (rpcServer2 != null) {
            rpcServer2.shutdown();
        }
    }

    @Test
    public void test1Client2Server() {
        RpcClient rpcClient = new RpcClient("list://127.0.0.1:8000,127.0.0.1:8001");
        EchoService echoService = BrpcProxy.getProxy(rpcClient, EchoService.class);
        Echo.EchoRequest request = Echo.EchoRequest.newBuilder().setMessage("hello").build();
        for (int i = 0; i < 100; i++) {
            echoService.echo(request);
        }
        rpcClient.stop();
    }

}
