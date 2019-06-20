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

import java.util.ArrayList;
import java.util.List;

import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.client.loadbalance.LoadBalanceStrategy;
import com.baidu.brpc.example.interceptor.CustomInterceptor;
import com.baidu.brpc.example.push.userservice.UserPushApiImpl;
import com.baidu.brpc.example.standard.EchoService;
import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.protocol.Options;
import com.baidu.brpc.server.entity.ReportBean;
import com.baidu.brpc.utils.GsonUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by wenweihu86 on 2017/4/26.
 */
@SuppressWarnings("unchecked")
@Slf4j
public class RpcClientServerPushTest {

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
        //        String serviceUrl = "zookeeper://127.0.0.1:2181";
        if (args.length == 1) {
            serviceUrl = args[0];
        }

        List<Interceptor> interceptors = new ArrayList<Interceptor>();
        interceptors.add(new CustomInterceptor());

        // 创建客户端 c1
        RpcClient rpcClient = new RpcClient(serviceUrl, clientOption, interceptors);
        // 首先建立一个普通rpc client服务, 与后端建立起连接
        EchoService echoService = BrpcProxy.getProxy(rpcClient, EchoService.class);
        // 注册实现push方法
        rpcClient.registerPushService(new UserPushApiImpl());

        //  Thread.sleep(1000);
        Object reportBean = echoService.echoReport(new ReportBean());
        System.out.println("c1 echoReport:" + GsonUtils.toJson(reportBean));

        Thread.sleep(2000);

        // 创建客户端c2
        clientOption.setClientName("c2");
        RpcClient rpcClient2 = new RpcClient(serviceUrl, clientOption, interceptors);
        EchoService echoService2 = BrpcProxy.getProxy(rpcClient2, EchoService.class);
        rpcClient2.registerPushService(new UserPushApiImpl());
        Object reportBean2 = echoService2.echoReport(new ReportBean());
        System.out.println("c2 echoReport:" + GsonUtils.toJson(reportBean));

        synchronized(RpcClientServerPushTest.class) {
            try {
                RpcClientServerPushTest.class.wait();
            } catch (Throwable e) {
            }
        }
        rpcClient.stop();
        rpcClient2.stop();
    }

}
