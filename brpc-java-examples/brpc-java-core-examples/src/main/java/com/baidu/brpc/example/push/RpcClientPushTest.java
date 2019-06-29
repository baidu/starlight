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

package com.baidu.brpc.example.push;

import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.client.loadbalance.LoadBalanceStrategy;
import com.baidu.brpc.example.push.normal.EchoRequest;
import com.baidu.brpc.example.push.normal.EchoResponse;
import com.baidu.brpc.example.push.normal.EchoService;
import com.baidu.brpc.example.push.normal.EchoService2;
import com.baidu.brpc.example.push.push.UserPushApiImpl;
import com.baidu.brpc.protocol.Options;

import com.baidu.brpc.utils.GsonUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by wenweihu86 on 2017/4/26.
 */
@SuppressWarnings("unchecked")
@Slf4j
public class RpcClientPushTest {

    public static void main(String[] args) throws InterruptedException {
        RpcClientOptions clientOption = new RpcClientOptions();
        clientOption.setProtocolType(Options.ProtocolType.PROTOCOL_SERVER_PUSH_VALUE);
        clientOption.setWriteTimeoutMillis(20 * 1000);
        clientOption.setReadTimeoutMillis(20 * 1000);
        clientOption.setMaxTotalConnections(1000);
        clientOption.setMinIdleConnections(1);
        clientOption.setLoadBalanceType(LoadBalanceStrategy.LOAD_BALANCE_FAIR);
        clientOption.setCompressType(Options.CompressType.COMPRESS_TYPE_NONE);
        clientOption.setMaxTotalConnections(1);

        // 指定clientName
        clientOption.setClientName("c1");

        String serviceUrl = "list://127.0.0.1:8002";
        if (args.length == 1) {
            serviceUrl = args[0];
        }

        // 创建客户端 c1
        RpcClient rpcClient = new RpcClient(serviceUrl, clientOption);
        // 首先建立一个普通rpc client服务, 与后端建立起连接
        final EchoService service1 = BrpcProxy.getProxy(rpcClient, EchoService.class);
        // 注册实现push方法
        rpcClient.registerPushService(new UserPushApiImpl());

        final EchoRequest request = new EchoRequest();
        request.setMessage("hello");

        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    EchoResponse response = service1.echo(request);
                    System.out.println("res1=" + GsonUtils.toJson(response));
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
        thread1.start();

        // 创建客户端c2
        clientOption.setClientName("c2");
        RpcClient rpcClient2 = new RpcClient(serviceUrl, clientOption);
        final EchoService2 service2 = BrpcProxy.getProxy(rpcClient2, EchoService2.class);
        rpcClient2.registerPushService(new UserPushApiImpl());

        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    EchoResponse response = service2.echo(request);
                    System.out.println("res=2" + GsonUtils.toJson(response));
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
        thread2.start();

        synchronized(RpcClientPushTest.class) {
            try {
                RpcClientPushTest.class.wait();
            } catch (Throwable e) {
            }
        }

        thread1.join();
        thread2.join();
        rpcClient.stop();
        rpcClient2.stop();
    }

}
