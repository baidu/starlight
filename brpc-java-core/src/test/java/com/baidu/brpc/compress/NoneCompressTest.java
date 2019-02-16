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

package com.baidu.brpc.compress;

import com.baidu.brpc.protocol.jprotobuf.EchoRequest;
import com.baidu.brpc.protocol.jprotobuf.EchoResponse;
import com.baidu.brpc.protocol.standard.Echo;
import com.baidu.brpc.protocol.standard.EchoService;
import com.baidu.brpc.JprotobufRpcMethodInfo;
import com.baidu.brpc.ProtobufRpcMethodInfo;
import io.netty.buffer.ByteBuf;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Method;

public class NoneCompressTest {
    private static ProtobufRpcMethodInfo protobufRpcMethodInfo;
    private static JprotobufRpcMethodInfo jprotobufRpcMethodInfo;
    private static NoneCompress compress;

    @BeforeClass
    public static void beforeClass() throws NoSuchMethodException {
        Method method = EchoService.class.getMethod("echo", Echo.EchoRequest.class);
        protobufRpcMethodInfo = new ProtobufRpcMethodInfo(method);
        Method method1 = com.baidu.brpc.protocol.jprotobuf.EchoService.class.getMethod("echo",
                EchoRequest.class);
        jprotobufRpcMethodInfo = new JprotobufRpcMethodInfo(method1);
        compress = new NoneCompress();
    }

    @Test
    public void testCompressInputForProtobuf() throws IOException {
        Echo.EchoRequest request = Echo.EchoRequest.newBuilder().setMessage("hello").build();
        ByteBuf byteBuf = compress.compressInput(request, protobufRpcMethodInfo);
        System.out.println(byteBuf.readableBytes());
    }

    @Test
    public void testUncompressInputForProtobuf() throws IOException {
        Echo.EchoRequest request = Echo.EchoRequest.newBuilder().setMessage("hello").build();
        ByteBuf byteBuf = compress.compressInput(request, protobufRpcMethodInfo);
        System.out.println(byteBuf.readableBytes());
        Echo.EchoRequest request2 = (Echo.EchoRequest) compress.uncompressInput(byteBuf, protobufRpcMethodInfo);
        System.out.println(request2.getMessage());
    }

    @Test
    public void testCompressInputForJprotobuf() throws IOException {
        EchoRequest request = new EchoRequest();
        request.setMessage("hello");
        ByteBuf byteBuf = compress.compressInput(request, jprotobufRpcMethodInfo);
        System.out.println(byteBuf.readableBytes());
    }

    @Test
    public void testProtobufEncodeRequestJprotobufDecodeRequest() throws IOException {
        Echo.EchoRequest request = Echo.EchoRequest.newBuilder().setMessage("hello").build();
        ByteBuf byteBuf = compress.compressInput(request, protobufRpcMethodInfo);
        System.out.println(byteBuf.readableBytes());

        Object object = compress.uncompressInput(byteBuf, jprotobufRpcMethodInfo);
        EchoRequest request2 = (EchoRequest) object;
        System.out.println(request2.getMessage());
    }

    @Test
    public void testJprotobufEncodeRequestProtobufDecodeRequest() throws IOException {
        EchoRequest request = new EchoRequest();
        request.setMessage("hello");
        ByteBuf byteBuf = compress.compressInput(request, jprotobufRpcMethodInfo);
        System.out.println(byteBuf.readableBytes());

        Object object = compress.uncompressInput(byteBuf, protobufRpcMethodInfo);
        Echo.EchoRequest request1 = (Echo.EchoRequest) object;
        System.out.println(request1.getMessage());
    }

    @Test
    public void testProtobufEncodeResponseJprotobufDecodeResponse() throws IOException {
        Echo.EchoResponse response = Echo.EchoResponse.newBuilder().setMessage("hello").build();
        ByteBuf byteBuf = compress.compressOutput(response, protobufRpcMethodInfo);
        System.out.println(byteBuf.readableBytes());

        Object object = compress.uncompressOutput(byteBuf, jprotobufRpcMethodInfo);
        EchoResponse response1 = (EchoResponse) object;
        System.out.println(response.getMessage());
    }

    @Test
    public void testJprotobufEncodeResponseProtobufDecodeResponse() throws IOException {
        EchoResponse response = new EchoResponse();
        response.setMessage("hello");
        ByteBuf byteBuf = compress.compressOutput(response, jprotobufRpcMethodInfo);
        System.out.println(byteBuf.readableBytes());

        Object object = compress.uncompressOutput(byteBuf, protobufRpcMethodInfo);
        Echo.EchoResponse response1 = (Echo.EchoResponse) object;
        System.out.println(response1.getMessage());
    }
}
