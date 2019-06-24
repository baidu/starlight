/*
 * Copyright (c) 2019 Baidu, Inc. All Rights Reserved.
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
package com.baidu.brpc.example.dubbo;

import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.naming.NamingOptions;
import com.baidu.brpc.protocol.Options;
import com.baidu.brpc.utils.GsonUtils;

public class DubboClient {

    public static void main(String[] args) {
        RpcClientOptions options = new RpcClientOptions();
        options.setProtocolType(Options.ProtocolType.PROTOCOL_DUBBO_VALUE);
        options.setReadTimeoutMillis(1000);
        options.setWriteTimeoutMillis(1000);
        RpcClient rpcClient = new RpcClient("dubbo://127.0.0.1:2181", options);

        NamingOptions namingOptions = new NamingOptions();
        namingOptions.setGroup("");
        namingOptions.setVersion("");

        EchoService echoService = RpcClient.getProxy(rpcClient, EchoService.class, namingOptions);

        for (int i = 0; i < 5; i++) {
            EchoRequest request = new EchoRequest();
            request.setMessage("hello world");
            EchoResponse response = echoService.echo(request);
            System.out.println("receive response:" + GsonUtils.toJson(response));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        rpcClient.stop();
    }
}
