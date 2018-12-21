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

package com.baidu.brpc.protocol.standard;

import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.buffer.DynamicCompositeByteBuf;
import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.protocol.Options;
import com.baidu.brpc.protocol.RpcRequest;
import com.baidu.brpc.utils.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.util.Map;

public class BaiduRpcProtocolTest {
    @Test
    public void testEncodeRequest() throws Exception {
        RpcRequest rpcRequest = buildRpcRequest();
        Assert.assertTrue(rpcRequest != null);
        ByteBuf buf = new BaiduRpcProtocol().encodeRequest(rpcRequest);
        Assert.assertTrue(buf.readableBytes() > 0);
        System.out.println(buf.readableBytes());
        System.out.println(ByteBufUtils.byteBufToString(buf));
    }

    @Test
    public void testDecode() throws Exception {
        RpcRequest rpcRequest = buildRpcRequest();
        ByteBuf buf = new BaiduRpcProtocol().encodeRequest(rpcRequest);
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf();
        compositeByteBuf.addBuffer(buf);
        Assert.assertTrue(buf.readableBytes() == compositeByteBuf.readableBytes());
        System.out.println(compositeByteBuf.toString());

        BaiduRpcDecodePacket packet = (BaiduRpcDecodePacket) new BaiduRpcProtocol().decode(compositeByteBuf);
        Assert.assertTrue(packet.getMetaBuf().readableBytes() > 0);
        System.out.println(packet.getMetaBuf());
        Assert.assertTrue(packet.getProtoAndAttachmentBuf().readableBytes() > 0);
        System.out.println(packet.getProtoAndAttachmentBuf());
    }

    public RpcRequest buildRpcRequest() throws Exception {
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setLogId(0);
        rpcRequest.setServiceName(EchoService.class.getName());
        rpcRequest.setTargetMethod(EchoService.class.getMethod("echo", Echo.EchoRequest.class));
        rpcRequest.setMethodName(rpcRequest.getTargetMethod().getName());
        rpcRequest.setTarget(new EchoServiceImpl());
        rpcRequest.setRpcMethodInfo(buildRpcMethodInfo());

        Echo.EchoRequest echoRequest = Echo.EchoRequest.newBuilder().setMessage("hello").build();
        rpcRequest.setArgs(new Object[] {echoRequest});
        rpcRequest.setBinaryAttachment(Unpooled.wrappedBuffer("hello".getBytes()));
        rpcRequest.setCompressType(Options.CompressType.COMPRESS_TYPE_NONE_VALUE);
        return rpcRequest;
    }

    public static RpcMethodInfo buildRpcMethodInfo() throws Exception {
        Class[] paramTypes = new Class[2];
        paramTypes[0] = RpcClient.class;
        paramTypes[1] = Class.class;
        Constructor constructor = BrpcProxy.class.getDeclaredConstructor(paramTypes);
        constructor.setAccessible(true);
        RpcClient rpcClient = new RpcClient("list://127.0.0.1:8002");
        BrpcProxy rpcProxy = (BrpcProxy) constructor.newInstance(rpcClient, EchoService.class);
        Map<String, RpcMethodInfo> methodInfoMap = rpcProxy.getRpcMethodMap();
        RpcMethodInfo rpcMethodInfo = methodInfoMap.entrySet().iterator().next().getValue();
        return rpcMethodInfo;
    }
}
