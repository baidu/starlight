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

package com.baidu.brpc.utils;

import com.baidu.brpc.protocol.jprotobuf.EchoRequest;
import com.baidu.brpc.protocol.standard.Echo;
import com.baidu.brpc.protocol.standard.EchoService;
import com.google.protobuf.Message;
import com.baidu.brpc.buffer.DynamicCompositeByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

public class ProtobufUtilsTest {
    @Test
    public void testGetMessageType() throws Exception {
        Method method = EchoService.class.getMethod("echo", Echo.EchoRequest.class);
        ProtobufUtils.MessageType messageType = ProtobufUtils.getMessageType(method);
        Assert.assertTrue(messageType == ProtobufUtils.MessageType.PROTOBUF);

        Method method2 = com.baidu.brpc.protocol.jprotobuf.EchoService.class.getMethod("echo", EchoRequest.class);
        ProtobufUtils.MessageType messageType2 = ProtobufUtils.getMessageType(method2);
        Assert.assertTrue(messageType2 == ProtobufUtils.MessageType.JPROTOBUF);
    }

    @Test
    public void testParseFrom() {
        Echo.EchoRequest request = Echo.EchoRequest.newBuilder().setMessage("hello").build();
        byte[] bytes = request.toByteArray();
        Echo.EchoRequest request1 = (Echo.EchoRequest) ProtobufUtils.parseFrom(bytes, request.getClass());
        Assert.assertTrue(request1.getMessage().equals(request.getMessage()));
    }

    @Test
    public void testParseFrom2() {
        Echo.EchoRequest request = Echo.EchoRequest.newBuilder().setMessage("hello").build();
        byte[] bytes = request.toByteArray();
        InputStream inputStream = new ByteBufInputStream(Unpooled.wrappedBuffer(bytes));
        Echo.EchoRequest request1 = (Echo.EchoRequest) ProtobufUtils.parseFrom(inputStream, request.getClass());
        Assert.assertTrue(request1.getMessage().equals(request.getMessage()));
    }

    @Test
    public void testParseFrom3() throws IOException {
        Echo.EchoRequest request = Echo.EchoRequest.newBuilder().setMessage("hello").build();
        byte[] bytes = request.toByteArray();
        ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        Message defaultInstance = request.getDefaultInstanceForType();
        Echo.EchoRequest request1 = (Echo.EchoRequest) ProtobufUtils.parseFrom(buf, defaultInstance);
        Assert.assertTrue(request1.getMessage().equals(request.getMessage()));
    }

    @Test
    public void testParseFrom4() throws IOException {
        Echo.EchoRequest request = Echo.EchoRequest.newBuilder().setMessage("hello").build();
        byte[] bytes = request.toByteArray();
        ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf(buf);
        Message defaultInstance = request.getDefaultInstanceForType();
        Echo.EchoRequest request1 = (Echo.EchoRequest) ProtobufUtils.parseFrom(compositeByteBuf, defaultInstance);
        Assert.assertTrue(request1.getMessage().equals(request.getMessage()));
    }
}
