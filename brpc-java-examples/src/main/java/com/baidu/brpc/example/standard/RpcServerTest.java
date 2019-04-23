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

import com.baidu.brpc.server.RpcServer;
import com.baidu.brpc.server.RpcServerOptions;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by wenweihu86 on 2017/4/25.
 */
@Slf4j
public class RpcServerTest {
    public static void main(String[] args) {
        int port = 8002;
        if (args.length == 1) {
            port = Integer.valueOf(args[0]);
        }

        RpcServerOptions options = new RpcServerOptions();
        options.setReceiveBufferSize(64 * 1024 * 1024);
        options.setSendBufferSize(64 * 1024 * 1024);
        options.setKeepAliveTime(20);
//        options.setNamingServiceUrl("zookeeper://127.0.0.1:2181");
//        final RpcServer rpcServer = new RpcServer(port, options, new ZookeeperNamingFactory());
        final RpcServer rpcServer = new RpcServer(port, options);
        rpcServer.registerService(new EchoServiceImpl());
        rpcServer.start();

        // make server keep running
        synchronized (RpcServerTest.class) {
            try {
                RpcServerTest.class.wait();
            } catch (Throwable e) {
            }
        }
    }
}
