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

package com.baidu.brpc.client.loadbalance;

import java.util.Random;

import com.baidu.brpc.interceptor.AbstractInterceptor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import com.baidu.brpc.protocol.standard.Echo;
import com.baidu.brpc.protocol.standard.EchoService;
import com.baidu.brpc.server.RpcServer;

public class LoadBalanceTest {

    private static RpcServer rpcServer1;
    private static RpcServer rpcServer2;
    private static RpcServer rpcServer3;
    private static String serviceUrl = "list://127.0.0.1:8000,127.0.0.1:8001,127.0.0.1:8002";

    @BeforeClass
    public static void beforeClass() {
        rpcServer1 = new RpcServer(8000);
        rpcServer1.registerService(new TestEchoService(100));
        rpcServer1.getInterceptors().add(new TestInterceptor(1));
        rpcServer1.start();
        rpcServer2 = new RpcServer(8001);
        rpcServer2.registerService(new TestEchoService(200));
        rpcServer2.getInterceptors().add(new TestInterceptor(2));
        rpcServer2.start();
        rpcServer3 = new RpcServer(8002);
        rpcServer3.registerService(new TestEchoService(300));
        rpcServer3.getInterceptors().add(new TestInterceptor(3));
        rpcServer3.start();
    }

    @AfterClass
    public static void afterClass() {
        if (rpcServer1 != null) {
            rpcServer1.shutdown();
        }
        if (rpcServer2 != null) {
            rpcServer2.shutdown();
        }
        if (rpcServer3 != null) {
            rpcServer3.shutdown();
        }
    }

    @Test
    public void testRandomStrategy() {
        RpcClientOptions clientOption = new RpcClientOptions();
        clientOption.setLoadBalanceType(LoadBalanceStrategy.LOAD_BALANCE_RANDOM);
        RpcClient rpcClient = new RpcClient(serviceUrl, clientOption, null);
        final Echo.EchoRequest request = Echo.EchoRequest.newBuilder().setMessage("hello").build();
        final EchoService echoService = BrpcProxy.getProxy(rpcClient, EchoService.class);
        for (int i = 0; i < 10; i++) {
            echoService.echo(request);
        }
        rpcClient.stop();
    }

    @Test
    public void testRoundRobinStrategy() {
        RpcClientOptions clientOption = new RpcClientOptions();
        clientOption.setLoadBalanceType(LoadBalanceType.ROUND_ROBIN.getId());
        RpcClient rpcClient = new RpcClient(serviceUrl, clientOption, null);
        final Echo.EchoRequest request = Echo.EchoRequest.newBuilder().setMessage("hello").build();
        final EchoService echoService = BrpcProxy.getProxy(rpcClient, EchoService.class);
        for (int i = 0; i < 10; i++) {
            echoService.echo(request);
        }
        rpcClient.stop();
    }

    @Test
    public void testWeightStrategy() {
        RpcClientOptions clientOption = new RpcClientOptions();
        clientOption.setLoadBalanceType(LoadBalanceType.WEIGHT.getId());
        RpcClient rpcClient = new RpcClient(serviceUrl, clientOption, null);
        final Echo.EchoRequest request = Echo.EchoRequest.newBuilder().setMessage("hello").build();
        final EchoService echoService = BrpcProxy.getProxy(rpcClient, EchoService.class);
        for (int i = 0; i < 10; i++) {
            echoService.echo(request);
        }
        rpcClient.stop();
    }

    @Test
    public void testFairStrategy() {
        RpcClientOptions clientOption = new RpcClientOptions();
        clientOption.setLatencyWindowSizeOfFairLoadBalance(10);
        clientOption.setLoadBalanceType(LoadBalanceType.FAIR.getId());
        RpcClient rpcClient = new RpcClient(serviceUrl, clientOption, null);
        final Echo.EchoRequest request = Echo.EchoRequest.newBuilder().setMessage("hello").build();
        final EchoService echoService = BrpcProxy.getProxy(rpcClient, EchoService.class);
        for (int i = 0; i < 20; i++) {
            echoService.echo(request);
        }
        rpcClient.stop();
    }

    static class TestEchoService implements EchoService {

        private Random random = new Random(System.currentTimeMillis());

        private int maxDelay;

        public TestEchoService(int maxDelay) {
            this.maxDelay = maxDelay;
        }

        @Override
        public Echo.EchoResponse echo(Echo.EchoRequest request) {
            String message = request.getMessage();
            Echo.EchoResponse response = Echo.EchoResponse.newBuilder().setMessage(message).build();
            try {
                Thread.sleep(random.nextInt(maxDelay));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return response;
        }
    }

    static class TestInterceptor extends AbstractInterceptor {

        private int serverId;

        public TestInterceptor(int serverId) {
            this.serverId = serverId;
        }

        @Override
        public boolean handleRequest(Request request) {
            System.out.println("------" + serverId + " called------");
            return true;
        }

        @Override
        public void handleResponse(Response response) {

        }
    }
}
