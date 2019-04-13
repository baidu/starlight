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

import com.baidu.brpc.Controller;
import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcCallback;
import com.baidu.brpc.client.RpcCallbackAdaptor;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.client.loadbalance.LoadBalanceType;
import com.baidu.brpc.example.interceptor.CustomInterceptor;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.protocol.Options;
import io.netty.util.ReferenceCountUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by huwenwei on 2019/4/9.
 */
@SuppressWarnings("unchecked")
public class RpcClientControllerTest {

    public static void main(String[] args) {
        RpcClientOptions clientOption = new RpcClientOptions();
        clientOption.setProtocolType(Options.ProtocolType.PROTOCOL_BAIDU_STD_VALUE);
        clientOption.setWriteTimeoutMillis(1000);
        clientOption.setReadTimeoutMillis(1000);
        clientOption.setMaxTotalConnections(1000);
        clientOption.setMinIdleConnections(10);
        clientOption.setLoadBalanceType(LoadBalanceType.FAIR.getId());
        clientOption.setCompressType(Options.CompressType.COMPRESS_TYPE_NONE);

        String serviceUrl = "list://127.0.0.1:8002";
//        String serviceUrl = "zookeeper://127.0.0.1:2181";
        if (args.length == 1) {
            serviceUrl = args[0];
        }

        List<Interceptor> interceptors = new ArrayList<Interceptor>();
        interceptors.add(new CustomInterceptor());

        // build request
        Echo.EchoRequest request = Echo.EchoRequest.newBuilder()
                .setMessage("helloooooooooooo")
                .build();

        // sync call
        RpcClient rpcClient = new RpcClient(serviceUrl, clientOption, interceptors);
//        RpcClient rpcClient = new RpcClient(serviceUrl, clientOption, interceptors, new ZookeeperNamingFactory());
        EchoServiceController echoService = BrpcProxy.getProxy(rpcClient, EchoServiceController.class);
        try {
            Controller controller = new Controller();
            controller.setRequestBinaryAttachment("example attachment".getBytes());
            Echo.EchoResponse response = echoService.echo(controller, request);
            System.out.printf("sync call service=EchoService.echo success, "
                            + "request=%s,response=%s\n",
                    request.getMessage(), response.getMessage());
            if (controller.getResponseBinaryAttachment() != null) {
                System.out.println("attachment="
                        + controller.getResponseBinaryAttachment().toString());
                ReferenceCountUtil.release(controller.getResponseBinaryAttachment());
            }
        } catch (RpcException ex) {
            System.out.println("sync call failed, ex=" + ex.getMessage());
        }
        rpcClient.stop();

        // async call
        rpcClient = new RpcClient(serviceUrl, clientOption, interceptors);
//        rpcClient = new RpcClient(serviceUrl, clientOption, interceptors, new ZookeeperNamingFactory());
        RpcCallback callback = new RpcCallbackAdaptor<Echo.EchoResponse>() {
            @Override
            public void success(Controller controller, Echo.EchoResponse response) {
                System.out.printf("async call EchoService.echo success, response=%s\n",
                        response.getMessage());
                if (controller.getResponseBinaryAttachment() != null) {
                    System.out.println("attachment="
                            + controller.getResponseBinaryAttachment().toString());
                    ReferenceCountUtil.release(controller.getResponseBinaryAttachment());
                }
            }

            @Override
            public void fail(Throwable e) {
                System.out.printf("async call EchoService.echo failed, %s\n", e.getMessage());
            }
        };
        EchoServiceControllerAsync asyncEchoService = BrpcProxy.getProxy(rpcClient, EchoServiceControllerAsync.class);
        try {
            Controller controller = new Controller();
            controller.setRequestBinaryAttachment("async example attachment".getBytes());
            Future<Echo.EchoResponse> future = asyncEchoService.echo(controller, request, callback);
            try {
                Echo.EchoResponse response = future.get(100, TimeUnit.MILLISECONDS);
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
