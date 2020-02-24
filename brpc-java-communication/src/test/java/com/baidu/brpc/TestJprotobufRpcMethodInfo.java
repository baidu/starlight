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

package com.baidu.brpc;

import com.baidu.brpc.protocol.jprotobuf.EchoRequest;
import com.baidu.brpc.protocol.jprotobuf.EchoResponse;
import com.baidu.brpc.protocol.jprotobuf.EchoService;
import com.baidu.brpc.protocol.standard.Echo;
import com.google.protobuf.CodedOutputStream;
import com.baidu.brpc.buffer.DynamicCompositeByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;

import java.io.OutputStream;
import java.lang.reflect.Method;

public class TestJprotobufRpcMethodInfo {
    @Test
    public void testProtobufRpcMethodInfo() throws Exception {
        Method method = EchoService.class.getMethod("echo", EchoRequest.class);
        JprotobufRpcMethodInfo rpcMethodInfo = new JprotobufRpcMethodInfo(method);
        Assert.assertTrue(rpcMethodInfo.getInputCodec() != null);
        Assert.assertTrue(rpcMethodInfo.getOutputCodec() != null);
    }

    @Test
    public void testInputEncode() throws Exception {
        Method method = EchoService.class.getMethod("echo", EchoRequest.class);
        JprotobufRpcMethodInfo rpcMethodInfo = new JprotobufRpcMethodInfo(method);


        EchoRequest request = new EchoRequest();
        request.setMessage("hello");
        byte[] bytes = rpcMethodInfo.inputEncode(request);
        Assert.assertTrue(bytes != null);
        Assert.assertTrue(bytes.length
                == Echo.EchoRequest.newBuilder().setMessage("hello").build().getSerializedSize());
    }

    @Test
    public void testInputWriteToStream() throws Exception {
        Method method = EchoService.class.getMethod("echo", EchoRequest.class);
        JprotobufRpcMethodInfo rpcMethodInfo = new JprotobufRpcMethodInfo(method);

        EchoRequest request = new EchoRequest();
        request.setMessage("hello");
        ByteBuf buf = Unpooled.buffer(64);
        OutputStream outputStream = new ByteBufOutputStream(buf);
        CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(outputStream);
        rpcMethodInfo.inputWriteToStream(request, codedOutputStream);
        Assert.assertTrue(buf.readableBytes()
                == Echo.EchoRequest.newBuilder().setMessage("hello").build().getSerializedSize());
    }

    @Test
    public void testOutputDecodeBytes() throws Exception {
        Method method = EchoService.class.getMethod("echo", EchoRequest.class);
        JprotobufRpcMethodInfo rpcMethodInfo = new JprotobufRpcMethodInfo(method);
        Echo.EchoResponse response = Echo.EchoResponse.newBuilder().setMessage("hello").build();
        byte[] bytes = response.toByteArray();
        EchoResponse response1 = (EchoResponse) rpcMethodInfo.outputDecode(bytes);
        Assert.assertTrue(response1.getMessage().equals(response.getMessage()));
    }

    @Test
    public void testOutputDecodeByteBuf() throws Exception {
        Method method = EchoService.class.getMethod("echo", EchoRequest.class);
        JprotobufRpcMethodInfo rpcMethodInfo = new JprotobufRpcMethodInfo(method);
        Echo.EchoResponse response = Echo.EchoResponse.newBuilder().setMessage("hello").build();
        byte[] bytes = response.toByteArray();
        ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        EchoResponse response1 = (EchoResponse) rpcMethodInfo.outputDecode(buf);
        Assert.assertTrue(response1.getMessage().equals(response.getMessage()));
    }

    @Test
    public void testOutputDecodeDynamicCompositeByteBuf() throws Exception {
        Method method = EchoService.class.getMethod("echo", EchoRequest.class);
        JprotobufRpcMethodInfo rpcMethodInfo = new JprotobufRpcMethodInfo(method);
        Echo.EchoResponse response = Echo.EchoResponse.newBuilder().setMessage("hello").build();
        byte[] bytes = response.toByteArray();
        ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf(buf);
        EchoResponse response1 = (EchoResponse) rpcMethodInfo.outputDecode(compositeByteBuf);
        Assert.assertTrue(response1.getMessage().equals(response.getMessage()));
    }

    @Test
    public void testInputDecode() throws Exception {
        Method method = EchoService.class.getMethod("echo", EchoRequest.class);
        JprotobufRpcMethodInfo rpcMethodInfo = new JprotobufRpcMethodInfo(method);

        Echo.EchoRequest request = Echo.EchoRequest.newBuilder().setMessage("hello").build();
        byte[] bytes = request.toByteArray();
        EchoRequest request1 = (EchoRequest) rpcMethodInfo.inputDecode(bytes);
        Assert.assertTrue(request1.getMessage().equals(request.getMessage()));
    }

    @Test
    public void testInputDecode2() throws Exception {
        Method method = EchoService.class.getMethod("echo", EchoRequest.class);
        JprotobufRpcMethodInfo rpcMethodInfo = new JprotobufRpcMethodInfo(method);

        Echo.EchoRequest request = Echo.EchoRequest.newBuilder().setMessage("hello").build();
        byte[] bytes = request.toByteArray();
        ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
        EchoRequest request1 = (EchoRequest) rpcMethodInfo.inputDecode(byteBuf);
        Assert.assertTrue(request1.getMessage().equals(request.getMessage()));
    }

    @Test
    public void testInputDecode3() throws Exception {
        Method method = EchoService.class.getMethod("echo", EchoRequest.class);
        JprotobufRpcMethodInfo rpcMethodInfo = new JprotobufRpcMethodInfo(method);

        Echo.EchoRequest request = Echo.EchoRequest.newBuilder().setMessage("hello").build();
        byte[] bytes = request.toByteArray();
        ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf(byteBuf);
        EchoRequest request1 = (EchoRequest) rpcMethodInfo.inputDecode(compositeByteBuf);
        Assert.assertTrue(request1.getMessage().equals(request.getMessage()));
    }

    @Test
    public void testOutputEncode() throws Exception {
        Method method = EchoService.class.getMethod("echo", EchoRequest.class);
        JprotobufRpcMethodInfo rpcMethodInfo = new JprotobufRpcMethodInfo(method);
        EchoResponse response = new EchoResponse();
        response.setMessage("hello");
        byte[] bytes = rpcMethodInfo.outputEncode(response);
        Assert.assertTrue(bytes.length
                == Echo.EchoResponse.newBuilder().setMessage("hello").build().toByteArray().length);
    }

    @Test
    public void testGetInputSerializedSize() throws Exception {
        Method method = EchoService.class.getMethod("echo", EchoRequest.class);
        JprotobufRpcMethodInfo rpcMethodInfo = new JprotobufRpcMethodInfo(method);
        EchoRequest request = new EchoRequest();
        request.setMessage("hello");
        Assert.assertTrue(rpcMethodInfo.getInputSerializedSize(request)
                == Echo.EchoRequest.newBuilder().setMessage("hello").build().getSerializedSize());
    }

    @Test
    public void testGetOutputSerializedSize() throws Exception {
        Method method = EchoService.class.getMethod("echo", EchoRequest.class);
        JprotobufRpcMethodInfo rpcMethodInfo = new JprotobufRpcMethodInfo(method);
        EchoResponse response = new EchoResponse();
        response.setMessage("hello");
        Assert.assertTrue(rpcMethodInfo.getOutputSerializedSize(response)
                == Echo.EchoResponse.newBuilder().setMessage("hello").build().getSerializedSize());
    }
}
