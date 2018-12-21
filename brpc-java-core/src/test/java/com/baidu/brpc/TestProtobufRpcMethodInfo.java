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

import com.baidu.brpc.buffer.DynamicCompositeByteBuf;
import com.baidu.brpc.protocol.standard.Echo;
import com.baidu.brpc.protocol.standard.EchoService;
import com.google.protobuf.CodedOutputStream;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;

import java.io.OutputStream;
import java.lang.reflect.Method;

public class TestProtobufRpcMethodInfo {
    @Test
    public void testProtobufRpcMethodInfo() throws Exception {
        Method method = EchoService.class.getMethod("echo", Echo.EchoRequest.class);
        ProtobufRpcMethodInfo rpcMethodInfo = new ProtobufRpcMethodInfo(method);
        Assert.assertTrue(rpcMethodInfo.getInputInstance() != null);
        Assert.assertTrue(rpcMethodInfo.getInputGetDefaultInstanceMethod() != null);
    }

    @Test
    public void testInputEncode() throws Exception {
        Method method = EchoService.class.getMethod("echo", Echo.EchoRequest.class);
        ProtobufRpcMethodInfo rpcMethodInfo = new ProtobufRpcMethodInfo(method);

        Echo.EchoRequest request = Echo.EchoRequest.newBuilder().setMessage("hello").build();
        byte[] bytes = rpcMethodInfo.inputEncode(request);
        Assert.assertTrue(bytes != null);
        Assert.assertTrue(bytes.length == request.getSerializedSize());
    }

    @Test
    public void testInputWriteToStream() throws Exception {
        Method method = EchoService.class.getMethod("echo", Echo.EchoRequest.class);
        ProtobufRpcMethodInfo rpcMethodInfo = new ProtobufRpcMethodInfo(method);

        Echo.EchoRequest request = Echo.EchoRequest.newBuilder().setMessage("hello").build();
        ByteBuf buf = Unpooled.buffer(request.getSerializedSize());
        OutputStream outputStream = new ByteBufOutputStream(buf);
        CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(outputStream);
        rpcMethodInfo.inputWriteToStream(request, codedOutputStream);
        Assert.assertTrue(buf.readableBytes() == request.getSerializedSize());
    }

    @Test
    public void testOutputDecodeBytes() throws Exception {
        Method method = EchoService.class.getMethod("echo", Echo.EchoRequest.class);
        ProtobufRpcMethodInfo rpcMethodInfo = new ProtobufRpcMethodInfo(method);
        Echo.EchoResponse response = Echo.EchoResponse.newBuilder().setMessage("hello").build();
        byte[] bytes = response.toByteArray();
        Echo.EchoResponse response1 = (Echo.EchoResponse) rpcMethodInfo.outputDecode(bytes);
        Assert.assertTrue(response1.getMessage().equals(response.getMessage()));
    }

    @Test
    public void testOutputDecodeByteBuf() throws Exception {
        Method method = EchoService.class.getMethod("echo", Echo.EchoRequest.class);
        ProtobufRpcMethodInfo rpcMethodInfo = new ProtobufRpcMethodInfo(method);
        Echo.EchoResponse response = Echo.EchoResponse.newBuilder().setMessage("hello").build();
        byte[] bytes = response.toByteArray();
        ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        Echo.EchoResponse response1 = (Echo.EchoResponse) rpcMethodInfo.outputDecode(buf);
        Assert.assertTrue(response1.getMessage().equals(response.getMessage()));
    }

    @Test
    public void testOutputDecodeDynamicCompositeByteBuf() throws Exception {
        Method method = EchoService.class.getMethod("echo", Echo.EchoRequest.class);
        ProtobufRpcMethodInfo rpcMethodInfo = new ProtobufRpcMethodInfo(method);
        Echo.EchoResponse response = Echo.EchoResponse.newBuilder().setMessage("hello").build();
        byte[] bytes = response.toByteArray();
        ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf(buf);
        Echo.EchoResponse response1 = (Echo.EchoResponse) rpcMethodInfo.outputDecode(compositeByteBuf);
        Assert.assertTrue(response1.getMessage().equals(response.getMessage()));
    }

    @Test
    public void testOutputDecodeStream() throws Exception {
        Method method = EchoService.class.getMethod("echo", Echo.EchoRequest.class);
        ProtobufRpcMethodInfo rpcMethodInfo = new ProtobufRpcMethodInfo(method);
        Echo.EchoResponse response = Echo.EchoResponse.newBuilder().setMessage("hello").build();
        byte[] bytes = response.toByteArray();
        ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        ByteBufInputStream inputStream = new ByteBufInputStream(buf);
        Echo.EchoResponse response1 = (Echo.EchoResponse) rpcMethodInfo.outputDecode(inputStream);
        Assert.assertTrue(response1.getMessage().equals(response.getMessage()));
    }

    @Test
    public void testInputDecode() throws Exception {
        Method method = EchoService.class.getMethod("echo", Echo.EchoRequest.class);
        ProtobufRpcMethodInfo rpcMethodInfo = new ProtobufRpcMethodInfo(method);

        Echo.EchoRequest request = Echo.EchoRequest.newBuilder().setMessage("hello").build();
        byte[] bytes = request.toByteArray();
        Echo.EchoRequest request1 = (Echo.EchoRequest) rpcMethodInfo.inputDecode(bytes);
        Assert.assertTrue(request1.getMessage().equals(request.getMessage()));
    }

    @Test
    public void testInputDecode2() throws Exception {
        Method method = EchoService.class.getMethod("echo", Echo.EchoRequest.class);
        ProtobufRpcMethodInfo rpcMethodInfo = new ProtobufRpcMethodInfo(method);

        Echo.EchoRequest request = Echo.EchoRequest.newBuilder().setMessage("hello").build();
        byte[] bytes = request.toByteArray();
        Echo.EchoRequest request1 = (Echo.EchoRequest) rpcMethodInfo.inputDecode(bytes, 0, bytes.length);
        Assert.assertTrue(request1.getMessage().equals(request.getMessage()));
    }

    @Test
    public void testInputDecode3() throws Exception {
        Method method = EchoService.class.getMethod("echo", Echo.EchoRequest.class);
        ProtobufRpcMethodInfo rpcMethodInfo = new ProtobufRpcMethodInfo(method);

        Echo.EchoRequest request = Echo.EchoRequest.newBuilder().setMessage("hello").build();
        byte[] bytes = request.toByteArray();
        ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
        Echo.EchoRequest request1 = (Echo.EchoRequest) rpcMethodInfo.inputDecode(byteBuf);
        Assert.assertTrue(request1.getMessage().equals(request.getMessage()));
    }

    @Test
    public void testInputDecode4() throws Exception {
        Method method = EchoService.class.getMethod("echo", Echo.EchoRequest.class);
        ProtobufRpcMethodInfo rpcMethodInfo = new ProtobufRpcMethodInfo(method);

        Echo.EchoRequest request = Echo.EchoRequest.newBuilder().setMessage("hello").build();
        byte[] bytes = request.toByteArray();
        ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf(byteBuf);
        Echo.EchoRequest request1 = (Echo.EchoRequest) rpcMethodInfo.inputDecode(compositeByteBuf);
        Assert.assertTrue(request1.getMessage().equals(request.getMessage()));
    }

    @Test
    public void testOutputEncode() throws Exception {
        Method method = EchoService.class.getMethod("echo", Echo.EchoRequest.class);
        ProtobufRpcMethodInfo rpcMethodInfo = new ProtobufRpcMethodInfo(method);
        Echo.EchoResponse response = Echo.EchoResponse.newBuilder().setMessage("hello").build();
        byte[] bytes = rpcMethodInfo.outputEncode(response);
        Assert.assertTrue(bytes.length == response.toByteArray().length);
    }

    @Test
    public void testGetInputSerializedSize() throws Exception {
        Method method = EchoService.class.getMethod("echo", Echo.EchoRequest.class);
        ProtobufRpcMethodInfo rpcMethodInfo = new ProtobufRpcMethodInfo(method);
        Echo.EchoRequest request = Echo.EchoRequest.newBuilder().setMessage("hello").build();
        Assert.assertTrue(rpcMethodInfo.getInputSerializedSize(request) == request.getSerializedSize());
    }

    @Test
    public void testGetOutputSerializedSize() throws Exception {
        Method method = EchoService.class.getMethod("echo", Echo.EchoRequest.class);
        ProtobufRpcMethodInfo rpcMethodInfo = new ProtobufRpcMethodInfo(method);
        Echo.EchoResponse response = Echo.EchoResponse.newBuilder().setMessage("hello").build();
        Assert.assertTrue(rpcMethodInfo.getOutputSerializedSize(response) == response.getSerializedSize());
    }
}
