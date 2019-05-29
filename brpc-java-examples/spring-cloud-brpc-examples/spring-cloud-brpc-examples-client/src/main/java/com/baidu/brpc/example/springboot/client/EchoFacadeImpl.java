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

package com.baidu.brpc.example.springboot.client;

import com.baidu.brpc.client.RpcCallback;
import com.baidu.brpc.example.springcloud.api.AsyncEchoService;
import com.baidu.brpc.example.springcloud.api.EchoRequest;
import com.baidu.brpc.example.springcloud.api.EchoResponse;
import com.baidu.brpc.example.springcloud.api.EchoService;
import com.baidu.brpc.spring.annotation.RpcProxy;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.concurrent.Future;

@Service
@Setter
@Getter
public class EchoFacadeImpl implements EchoFacade {
    @RpcProxy(name = "brpc-example-server")
    private EchoService echoService;

    @RpcProxy(name = "brpc-example-server")
    private EchoService echoService2;

    @RpcProxy(name = "brpc-example-server")
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
