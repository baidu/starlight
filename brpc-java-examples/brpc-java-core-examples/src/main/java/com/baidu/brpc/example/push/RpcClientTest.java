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
import com.baidu.brpc.example.interceptor.CustomInterceptor;
import com.baidu.brpc.example.push.userservice.UserPushApiImpl;
import com.baidu.brpc.example.push.userservice.UserPushApiImplII;
import com.baidu.brpc.example.standard.EchoService;
import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.protocol.Options;
import com.baidu.brpc.server.entity.ReportBean;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wenweihu86 on 2017/4/26.
 */
@SuppressWarnings("unchecked")
@Slf4j
public class RpcClientTest {

    public static void main(String[] args) throws InterruptedException {
        RpcClientOptions clientOption = new RpcClientOptions();
        clientOption.setProtocolType(Options.ProtocolType.PROTOCOL_SERVER_PUSH_VALUE);
        clientOption.setWriteTimeoutMillis(20 * 1000);
        clientOption.setReadTimeoutMillis(20 * 1000);
        clientOption.setMaxTotalConnections(1000);
//        clientOption.setMaxTotalConnections(1);
        clientOption.setMinIdleConnections(1);
        clientOption.setLoadBalanceType(LoadBalanceStrategy.LOAD_BALANCE_FAIR);
        clientOption.setCompressType(Options.CompressType.COMPRESS_TYPE_NONE);


        String serviceUrl = "list://127.0.0.1:8012";

        if (args.length == 1) {
            serviceUrl = args[0];
        }

        List<Interceptor> interceptors = new ArrayList<Interceptor>();
        interceptors.add(new CustomInterceptor());

        clientOption.setClientName("Benchmark");
        RpcClient rpcClientII = new RpcClient(serviceUrl, clientOption, interceptors);
        BrpcProxy.getProxy(rpcClientII, EchoService.class);
        rpcClientII.registerPushService(new UserPushApiImplII());

        synchronized(RpcClientTest.class) {
            try {
                RpcClientTest.class.wait();
            } catch (Throwable e) {
            }
        }

        rpcClientII.stop();
    }

}
