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

import com.baidu.brpc.example.push.normal.EchoService2Impl;
import com.baidu.brpc.example.push.normal.EchoServiceImpl;
import com.baidu.brpc.example.push.push.PushData;
import com.baidu.brpc.example.push.push.PushResult;
import com.baidu.brpc.example.push.push.ServerSideUserPushApi;
import com.baidu.brpc.protocol.Options;
import com.baidu.brpc.server.BrpcPushProxy;
import com.baidu.brpc.server.RpcServer;
import com.baidu.brpc.server.RpcServerOptions;
import com.baidu.brpc.utils.GsonUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by wenweihu86 on 2017/4/25.
 */
@Slf4j
public class RpcServerPushTest {

    public static void main(String[] args) throws InterruptedException {
        int port = 8002;
        if (args.length == 1) {
            port = Integer.valueOf(args[0]);
        }

        RpcServerOptions options = new RpcServerOptions();
        options.setReceiveBufferSize(64 * 1024 * 1024);
        options.setSendBufferSize(64 * 1024 * 1024);
        options.setKeepAliveTime(20);
        options.setProtocolType(Options.ProtocolType.PROTOCOL_SERVER_PUSH_VALUE);
//        options.setNamingServiceUrl("zookeeper://127.0.0.1:2181");

        final RpcServer rpcServer = new RpcServer(port, options);
        rpcServer.registerService(new EchoServiceImpl());
        rpcServer.registerService(new EchoService2Impl());

        // get push api
        ServerSideUserPushApi proxyPushApi = BrpcPushProxy.getProxy(rpcServer, ServerSideUserPushApi.class);
        rpcServer.start();

        // wait until the client connected to server
        while (!EchoServiceImpl.clientStarted || !EchoService2Impl.client2Started) {
            Thread.sleep(1000);
        }

        // test clientname exist or not
        try {
            PushData p1 = new PushData();
            p1.setData("hellooooo");
            PushResult pushResultI = proxyPushApi.clientReceive("c3","c3", p1);
            log.info("push result: {}" , GsonUtils.toJson(pushResultI) );
        } catch (Exception e) {
            log.error("case one--clientname not exist, exception: {}", e.getMessage());
        }

        // push data to 2 clients : "c1" and "c2"
        int i = 0;
        while (true) {
            i++;
            PushData p = new PushData();
            p.setData("pushData" + i);
            int index = i % 2 + 1;
            String clientName = "c" + index;
            String extra = "c" + (index + 100);
            log.info("pushing data to client:" + clientName);
            try {
                // last param of api is clientName
                PushResult pushResult = proxyPushApi.clientReceive(clientName, extra, p);
                log.info("received push result:" + GsonUtils.toJson(pushResult));
            } catch (Exception e) {
                log.error("push exception , please start up client c1 and c2", e);
            }

            Thread.sleep(1000);
        }

    }
}
