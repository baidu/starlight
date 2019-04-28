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

package com.baidu.brpc.protocol.nshead;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;

import com.baidu.brpc.ProtobufRpcMethodInfo;
import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.protocol.Options.ProtocolType;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.RpcRequest;
import com.baidu.brpc.protocol.RpcResponse;
import com.baidu.brpc.protocol.standard.Echo;
import com.baidu.brpc.protocol.standard.Echo.EchoRequest;
import com.baidu.brpc.protocol.standard.EchoService;
import com.baidu.brpc.protocol.standard.EchoServiceImpl;
import com.baidu.brpc.server.ServiceManager;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class NsHeadRpcProtocolProtobufTest {

    private NSHeadRpcProtocol protocol = new NSHeadRpcProtocol(ProtocolType.PROTOCOL_NSHEAD_PROTOBUF_VALUE,
            "utf-8");
    @Before
    public void init() {
        if (ServiceManager.getInstance() != null) {
            ServiceManager.getInstance().getServiceMap().clear();
        }
    }

    @Test
    public void testEncodeRequest() throws Exception {

        EchoRequest request = Echo.EchoRequest.newBuilder().setMessage("hello").build();

        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setArgs(new Object[] {request});
        rpcRequest.setRpcMethodInfo(new ProtobufRpcMethodInfo(EchoService.class.getMethods()[0]));
        rpcRequest.setLogId(3L);
        NSHeadMeta nsHeadMeta = rpcRequest.getRpcMethodInfo().getNsHeadMeta();
        rpcRequest.setNsHead(new NSHead(3, nsHeadMeta.id(),
                nsHeadMeta.version(), nsHeadMeta.provider(), 0));

        ByteBuf byteBuf = protocol.encodeRequest(rpcRequest);

        NSHead nsHead = NSHead.fromByteBuf(byteBuf);

        assertEquals(3, nsHead.logId);
        assertEquals("", nsHead.provider);
        assertEquals(byteBuf.readableBytes(), nsHead.bodyLength);
    }


    @Test
    public void testDecodeRequestSuccess() throws Exception {
        ServiceManager.getInstance().getServiceMap().clear();

        ServiceManager.getInstance().registerService(new EchoServiceImpl(), null);
        Echo.EchoResponse response = Echo.EchoResponse.newBuilder()
                .setMessage("hello").build();

        NSHeadPacket packet = new NSHeadPacket();
        byte[] body = encodeBody(response, EchoService.class.getMethods()[0]);
        packet.setNsHead(new NSHead(1, body.length));
        packet.setBodyBuf(Unpooled.wrappedBuffer(body));
        Request request = protocol.decodeRequest(packet);

        assertEquals(EchoService.class.getMethods()[0], request.getTargetMethod());
        assertEquals(EchoServiceImpl.class, request.getTarget().getClass());
    }


    @Test
    public void testEncodeResponse() throws Exception {

        Echo.EchoResponse response = Echo.EchoResponse.newBuilder()
                .setMessage("hello").build();

        RpcResponse rpcResponse = new RpcResponse();
        rpcResponse.setLogId(4L);
        rpcResponse.setRpcMethodInfo(new ProtobufRpcMethodInfo(EchoService.class.getMethods()[0]));
        rpcResponse.setResult(response);

        ByteBuf byteBuf = protocol.encodeResponse(null, rpcResponse);

        NSHead nsHead = NSHead.fromByteBuf(byteBuf);

        assertEquals(4, nsHead.logId);
        assertEquals(byteBuf.readableBytes(), nsHead.bodyLength);
    }

    private byte[] encodeBody(Object body, Method invokeMethod) throws Exception {

        Method method = protocol.getClass().getDeclaredMethod("encodeBody", Object.class, RpcMethodInfo.class);
        method.setAccessible(true);
        Object r = method.invoke(protocol, body, new ProtobufRpcMethodInfo(invokeMethod));

        return (byte[]) r;
    }
}
