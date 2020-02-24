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

package com.baidu.brpc.push;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.baidu.brpc.RpcOptionsUtils;
import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.protocol.Options;
import com.baidu.brpc.protocol.standard.Echo;
import com.baidu.brpc.protocol.standard.EchoService;
import com.baidu.brpc.protocol.standard.EchoServiceImpl;
import com.baidu.brpc.push.userservice.PushData;
import com.baidu.brpc.push.userservice.PushResult;
import com.baidu.brpc.push.userservice.ServerSideUserPushApi;
import com.baidu.brpc.push.userservice.UserPushApiImpl;
import com.baidu.brpc.server.BrpcPushProxy;
import com.baidu.brpc.server.RpcServer;
import com.baidu.brpc.server.RpcServerOptions;
import com.baidu.brpc.server.ServiceManager;

public class ServerPushTest {

    @Before
    public void init() {
        if (ServiceManager.getInstance() != null) {
            ServiceManager.getInstance().getServiceMap().clear();
        }
    }

    @Test
    public void testBasic() {
        RpcServerOptions rpcServerOptions = RpcOptionsUtils.getRpcServerOptions();
        rpcServerOptions.setProtocolType(Options.ProtocolType.PROTOCOL_SERVER_PUSH_VALUE);
        RpcServer rpcServer = new RpcServer(8000, rpcServerOptions);
        rpcServer.registerService(new EchoServiceImpl());
        rpcServer.start();

        RpcClientOptions rpcClientOptions = RpcOptionsUtils.getRpcClientOptions();
        rpcClientOptions.setProtocolType(Options.ProtocolType.PROTOCOL_SERVER_PUSH_VALUE);
        rpcClientOptions.setClientName("c1");
        RpcClient rpcClient = new RpcClient("list://127.0.0.1:8000", rpcClientOptions);
        EchoService echoService = BrpcProxy.getProxy(rpcClient, EchoService.class);
        rpcClient.registerPushService(new UserPushApiImpl());

        Echo.EchoRequest request = Echo.EchoRequest.newBuilder().setMessage("hello").build();
        Echo.EchoResponse response = echoService.echo(request);
        assertEquals("hello", response.getMessage());

        ServerSideUserPushApi pushApi =
                (ServerSideUserPushApi) BrpcPushProxy.getProxy(rpcServer, ServerSideUserPushApi.class);
        PushData p = new PushData();
        p.setData("abc");
        PushResult pushResult = pushApi.clientReceive("c1", p);

        assertEquals("got data:abc", pushResult.getResult());

        rpcClient.stop();

        rpcServer.shutdown();
    }

}
