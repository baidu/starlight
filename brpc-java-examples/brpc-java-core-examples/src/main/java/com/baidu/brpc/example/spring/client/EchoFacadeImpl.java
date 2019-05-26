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

package com.baidu.brpc.example.spring.client;

import com.baidu.brpc.client.RpcCallback;
import com.baidu.brpc.example.spring.api.AsyncEchoService;
import com.baidu.brpc.example.spring.api.EchoRequest;
import com.baidu.brpc.example.spring.api.EchoResponse;
import com.baidu.brpc.example.spring.api.EchoService;
import com.baidu.brpc.spring.annotation.RpcProxy;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.concurrent.Future;

@Service
@Setter
@Getter
public class EchoFacadeImpl implements EchoFacade {
    @RpcProxy(rpcClientOptionsBeanName = "rpcClientOptions",
            interceptorBeanName = "customInterceptor")
    private EchoService echoService;

    @RpcProxy(rpcClientOptionsBeanName = "rpcClientOptions",
            interceptorBeanName = "customInterceptor")
    private EchoService echoService2;

    /**
     * async service interface proxy will create new RpcClient,
     * not used RpcClient of sync interface proxy.
     */
    @RpcProxy(rpcClientOptionsBeanName = "rpcClientOptions",
            interceptorBeanName = "customInterceptor")
    private AsyncEchoService echoService3;

    public EchoResponse echo(EchoRequest request) {
        System.out.println(echoService.hashCode());
        return echoService.echo(request);
    }

    public EchoResponse echo2(EchoRequest request) {
        System.out.println(echoService2.hashCode());
        return echoService2.echo(request);
    }

    public Future<EchoResponse> echo3(EchoRequest request) {
        System.out.println(echoService3.hashCode());
        Future<EchoResponse> future = echoService3.echo(request, new RpcCallback<EchoResponse>() {
            @Override
            public void success(EchoResponse response) {
                System.out.println(response.getMessage());
            }

            @Override
            public void fail(Throwable e) {
                e.printStackTrace();
            }
        });
        return future;
    }
}
