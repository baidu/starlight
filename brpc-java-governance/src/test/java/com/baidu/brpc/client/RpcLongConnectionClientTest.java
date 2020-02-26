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

package com.baidu.brpc.client;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.baidu.brpc.protocol.Options;
import com.baidu.brpc.protocol.standard.Echo;
import com.baidu.brpc.protocol.standard.EchoService;
import com.baidu.brpc.protocol.standard.EchoServiceImpl;
import com.baidu.brpc.RpcOptionsUtils;
import com.baidu.brpc.server.RpcServer;
import com.baidu.brpc.server.RpcServerOptions;
import com.baidu.brpc.server.ServiceManager;

public class RpcLongConnectionClientTest {

    @Before
    public void init() {
        if (ServiceManager.getInstance() != null) {
            ServiceManager.getInstance().getServiceMap().clear();
        }
    }

    @Test
    public void testBasic() {
        RpcServer rpcServer = new RpcServer(8000, RpcOptionsUtils.getRpcServerOptions());
        rpcServer.registerService(new EchoServiceImpl());
        rpcServer.start();

        RpcClient rpcClient = new RpcClient("list://127.0.0.1:8000",
                RpcOptionsUtils.getRpcClientOptions());
        EchoService echoService = BrpcProxy.getProxy(rpcClient, EchoService.class);
        Echo.EchoRequest request = Echo.EchoRequest.newBuilder().setMessage("hello").build();
        Echo.EchoResponse response = echoService.echo(request);
        assertEquals("hello", response.getMessage());
        rpcClient.stop();

        rpcServer.shutdown();
    }

    @Test
    public void testHttpProto() {
        RpcServerOptions serverOptions = RpcOptionsUtils.getRpcServerOptions();
        serverOptions.setProtocolType(Options.ProtocolType.PROTOCOL_HTTP_PROTOBUF_VALUE);
        RpcServer rpcServer = new RpcServer(8000, serverOptions);
        rpcServer.registerService(new EchoServiceImpl());
        rpcServer.start();

        RpcClientOptions clientOptions = RpcOptionsUtils.getRpcClientOptions();
        clientOptions.setProtocolType(Options.ProtocolType.PROTOCOL_HTTP_PROTOBUF_VALUE);
        clientOptions.setMaxTryTimes(1);
        clientOptions.setReadTimeoutMillis(1000000);
        clientOptions.setWriteTimeoutMillis(1000000);
        RpcClient rpcClient = new RpcClient("list://127.0.0.1:8000", clientOptions);
        EchoService echoService = BrpcProxy.getProxy(rpcClient, EchoService.class);

        // test big message
        String message =
                "hellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohello"
                        + "hellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohello"
                        + "hellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohello"
                        + "hellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohello"
                        + "hellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohello"
                        + "hellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohello"
                        + "hellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohello"
                        + "hellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohello"
                        + "hellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohello"
                        + "hellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohello"
                        + "hellohellohellohellohellohellohellohellohellohellohellohellohellohellohellohello";
        Echo.EchoRequest request = Echo.EchoRequest.newBuilder().setMessage(message).build();
        Echo.EchoResponse response = echoService.echo(request);
        assertEquals(message, response.getMessage());

        // test small message
        message = "hello";
        request = Echo.EchoRequest.newBuilder().setMessage(message).build();
        response = echoService.echo(request);
        assertEquals(message, response.getMessage());

        rpcClient.stop();
        rpcServer.shutdown();
    }

    @Test
    public void testNsheadProto() {
        RpcServerOptions serverOptions = RpcOptionsUtils.getRpcServerOptions();
        serverOptions.setProtocolType(Options.ProtocolType.PROTOCOL_NSHEAD_PROTOBUF_VALUE);
        RpcServer rpcServer = new RpcServer(8000, serverOptions);
        rpcServer.registerService(new EchoServiceImpl());
        rpcServer.start();

        RpcClientOptions clientOptions = RpcOptionsUtils.getRpcClientOptions();
        clientOptions.setProtocolType(Options.ProtocolType.PROTOCOL_NSHEAD_PROTOBUF_VALUE);
        RpcClient rpcClient = new RpcClient("list://127.0.0.1:8000", clientOptions);
        EchoService echoService = BrpcProxy.getProxy(rpcClient, EchoService.class);
        Echo.EchoRequest request = Echo.EchoRequest.newBuilder().setMessage("hello").build();
        Echo.EchoResponse response = echoService.echo(request);
        assertEquals("hello", response.getMessage());

        rpcClient.stop();
        rpcServer.shutdown();
    }
}
