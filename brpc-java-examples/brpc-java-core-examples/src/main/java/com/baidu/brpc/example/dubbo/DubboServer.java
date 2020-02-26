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

import com.baidu.brpc.protocol.NamingOptions;
import com.baidu.brpc.protocol.Options;
import com.baidu.brpc.server.RpcServer;
import com.baidu.brpc.server.RpcServerOptions;

public class DubboServer {

    public static void main(String[] args) {
        RpcServerOptions serverOptions = new RpcServerOptions();
        serverOptions.setProtocolType(Options.ProtocolType.PROTOCOL_DUBBO_VALUE);
        serverOptions.setNamingServiceUrl("dubbo://127.0.0.1:2181");
        serverOptions.setReceiveBufferSize(64 * 1024 * 1024);
        serverOptions.setSendBufferSize(64 * 1024 * 1024);
        serverOptions.setWorkThreadNum(12);

        RpcServer rpcServer = new RpcServer(8898, serverOptions);
        EchoService service = new EchoServiceImpl();

        NamingOptions namingOptions = new NamingOptions();
        namingOptions.setGroup("");
        namingOptions.setVersion("");
        rpcServer.registerService(service, namingOptions);
        rpcServer.start();

        // make server keep running
        synchronized (DubboServer.class) {
            try {
                DubboServer.class.wait();
            } catch (Throwable e) {
            }
        }
    }

}
