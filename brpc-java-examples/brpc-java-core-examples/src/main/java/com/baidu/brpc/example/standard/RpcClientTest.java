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

package com.baidu.brpc.example.standard;

import com.baidu.brpc.RpcContext;
import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcCallback;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.loadbalance.LoadBalanceStrategy;
import com.baidu.brpc.example.interceptor.CustomInterceptor;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.protocol.Options;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by wenweihu86 on 2017/4/26.
 */
@SuppressWarnings("unchecked")
@Slf4j
public class RpcClientTest {

    public static void main(String[] args) {
        RpcClientOptions clientOption = new RpcClientOptions();
        clientOption.setProtocolType(Options.ProtocolType.PROTOCOL_BAIDU_STD_VALUE);
        clientOption.setWriteTimeoutMillis(1000);
        clientOption.setReadTimeoutMillis(50000);
        clientOption.setMaxTotalConnections(1000);
        clientOption.setMinIdleConnections(10);
        clientOption.setLoadBalanceType(LoadBalanceStrategy.LOAD_BALANCE_FAIR);
        clientOption.setCompressType(Options.CompressType.COMPRESS_TYPE_NONE);

        String serviceUrl = "list://127.0.0.1:8002";
//        String serviceUrl = "consul://127.0.0.1:8500";
        if (args.length == 1) {
            serviceUrl = args[0];
        }

        // build request
        Echo.EchoRequest request = Echo.EchoRequest.newBuilder()
                .setMessage("helloooooooooooo")
                .build();

        List<Interceptor> interceptors = new ArrayList<Interceptor>();
        interceptors.add(new CustomInterceptor());

        // sync call
        RpcClient rpcClient = new RpcClient(serviceUrl, clientOption, interceptors);
//        RpcClient rpcClient = new RpcClient(serviceUrl, clientOption, interceptors);
        EchoService echoService = BrpcProxy.getProxy(rpcClient, EchoService.class);
        RpcContext.getContext().setLogId(1234L);
        try {
            Echo.EchoResponse response = echoService.echo(request);
            System.out.printf("sync call service=EchoService.echo success, "
                            + "request=%s,response=%s\n",
                    request.getMessage(), response.getMessage());
        } catch (RpcException ex) {
            log.warn("sync call failed, ex=", ex);
        }
        rpcClient.stop();

        // async call
        rpcClient = new RpcClient(serviceUrl, clientOption, interceptors);
//        rpcClient = new RpcClient(serviceUrl, clientOption, interceptors);
        RpcCallback callback = new RpcCallback<Echo.EchoResponse>() {
            @Override
            public void success(Echo.EchoResponse response) {
                System.out.printf("async call EchoService.echo success, response=%s\n",
                        response.getMessage());
            }

            @Override
            public void fail(Throwable e) {
                System.out.printf("async call EchoService.echo failed, %s\n", e.getMessage());
            }
        };
        EchoServiceAsync asyncEchoService = BrpcProxy.getProxy(rpcClient, EchoServiceAsync.class);
        try {
            Future<Echo.EchoResponse> future = asyncEchoService.echo(request, callback);
            try {
                Echo.EchoResponse response = future.get(1000, TimeUnit.MILLISECONDS);
                System.out.println("response from future:" + response.getMessage());
            } catch (Exception ex1) {
                ex1.printStackTrace();
            }
        } catch (RpcException ex) {
            System.out.println("rpc send failed, ex=" + ex.getMessage());
        }
        rpcClient.stop();
    }

}
