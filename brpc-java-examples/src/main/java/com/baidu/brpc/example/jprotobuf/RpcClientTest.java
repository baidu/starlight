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

package com.baidu.brpc.example.jprotobuf;

import com.baidu.brpc.client.loadbalance.LoadBalanceStrategy;
import com.baidu.brpc.example.interceptor.CustomInterceptor;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.client.RpcCallback;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.protocol.Options;
import io.netty.channel.Channel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by wenweihu86 on 2017/4/26.
 */
@SuppressWarnings("unchecked")
public class RpcClientTest {

    public static void main(String[] args) {
        RpcClientOptions clientOption = new RpcClientOptions();
        clientOption.setProtocolType(Options.ProtocolType.PROTOCOL_BAIDU_STD_VALUE);
        clientOption.setWriteTimeoutMillis(1000);
        clientOption.setReadTimeoutMillis(1000);
        clientOption.setMaxTotalConnections(1000);
        clientOption.setMinIdleConnections(10);
//        clientOption.setIoThreadNum(40);
        clientOption.setLoadBalanceType(LoadBalanceStrategy.LOAD_BALANCE_FAIR);
        clientOption.setCompressType(Options.CompressType.COMPRESS_TYPE_NONE);

        String serviceUrl = "list://127.0.0.1:8002";
        if (args.length == 1) {
            serviceUrl = args[0];
        }

        List<Interceptor> interceptors = new ArrayList<Interceptor>();;
        interceptors.add(new CustomInterceptor());
        RpcClient rpcClient = new RpcClient(serviceUrl, clientOption, interceptors);

        // build request
        EchoRequest request = new EchoRequest();
        request.setMessage("hellooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo");

        // sync call
        EchoService echoService = BrpcProxy.getProxy(rpcClient, EchoService.class);
        Channel channel = null;
        try {
            EchoResponse response = echoService.echo(request);
            System.out.printf("sync call service=EchoService.echo success, "
                            + "request=%s,response=%s\n",
                    request.getMessage(), response.getMessage());
        } catch (RpcException ex) {
            System.out.println("sync call failed, ex=" + ex.getMessage());
        }
        rpcClient.stop();

        // async call
        rpcClient = new RpcClient(serviceUrl, clientOption, interceptors);
        RpcCallback callback = new RpcCallback<EchoResponse>() {
            @Override
            public void success(EchoResponse response) {
                if (response != null) {
                    System.out.printf("async call EchoService.echo success, response=%s\n",
                            response.getMessage());
                } else {
                    System.out.println("async call failed, service=EchoService.echo");
                }
            }

            @Override
            public void fail(Throwable e) {
                System.out.printf("async call EchoService.echo failed, %s\n", e.getMessage());
            }
        };
        EchoServiceAsync asyncEchoService = BrpcProxy.getProxy(rpcClient, EchoServiceAsync.class);
        try {
            Future<EchoResponse> future = asyncEchoService.echo(request, callback);
            try {
                if (future != null) {
                    future.get();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } catch (RpcException ex) {
            System.out.println("rpc send failed, ex=" + ex.getMessage());
        }
        rpcClient.stop();
    }

}
