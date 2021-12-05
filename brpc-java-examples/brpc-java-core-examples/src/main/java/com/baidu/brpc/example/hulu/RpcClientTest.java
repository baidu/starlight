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

package com.baidu.brpc.example.hulu;

import com.baidu.brpc.RpcContext;
import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcCallback;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.example.interceptor.CustomInterceptor;
import com.baidu.brpc.example.standard.Echo;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.loadbalance.LoadBalanceStrategy;
import com.baidu.brpc.protocol.Options;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unchecked")
@Slf4j
public class RpcClientTest {
    private static final Logger LOG = LoggerFactory.getLogger(RpcClientTest.class);

    public static void main(String[] args) {
        RpcClientOptions clientOption = new RpcClientOptions();
        clientOption.setProtocolType(Options.ProtocolType.PROTOCOL_HULU_PBRPC_VALUE);
        clientOption.setWriteTimeoutMillis(1000);
        clientOption.setReadTimeoutMillis(50000);
        clientOption.setMaxTotalConnections(1000);
        clientOption.setMinIdleConnections(10);
        clientOption.setLoadBalanceType(LoadBalanceStrategy.LOAD_BALANCE_FAIR);
        clientOption.setCompressType(Options.CompressType.COMPRESS_TYPE_NONE);

        String serviceUrl = "list://127.0.0.1:8002";
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
        EchoServiceHulu echoService = BrpcProxy.getProxy(rpcClient, EchoServiceHulu.class);
        RpcContext.getContext().setLogId(1234L);
        RpcContext.getContext().setRequestBinaryAttachment("zb".getBytes());
        try {
            Echo.EchoResponse response = echoService.echo(request);
            LOG.info("sync call service=EchoService.echo success, "
                            + "request={},response={}",
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
                LOG.info("async call EchoService.echo success, response={}",
                        response.getMessage());
            }

            @Override
            public void fail(Throwable e) {
                LOG.info("async call EchoService.echo failed, {}", e.getMessage());
            }
        };
        EchoServiceAsyncHulu asyncEchoService = BrpcProxy.getProxy(rpcClient, EchoServiceAsyncHulu.class);
        try {
            RpcContext.getContext().setRequestBinaryAttachment("zb".getBytes());
            Future<Echo.EchoResponse> future = asyncEchoService.echo(request, callback);
            try {
                future.get(1000, TimeUnit.MILLISECONDS);
                RpcContext context = RpcContext.getContext();
                ByteBuf attachment = context.getResponseBinaryAttachment();
                if (attachment != null) {
                    ByteBuf byteBuf = Unpooled.copiedBuffer(attachment);
                    String attachmentString = new String(byteBuf.array());
                    LOG.info("async call attachment:{}", attachmentString);
                }
            } catch (Exception ex1) {
                ex1.printStackTrace();
            }
        } catch (RpcException ex) {
            LOG.info("rpc send failed, ex=" + ex.getMessage());
        }
        rpcClient.stop();
    }

}
