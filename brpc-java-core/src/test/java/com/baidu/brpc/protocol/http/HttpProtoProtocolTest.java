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

package com.baidu.brpc.protocol.http;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;

import com.baidu.brpc.ProtobufRpcMethodInfo;
import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.protocol.HttpRequest;
import com.baidu.brpc.protocol.HttpResponse;
import com.baidu.brpc.protocol.Options.ProtocolType;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import com.baidu.brpc.protocol.standard.Echo;
import com.baidu.brpc.protocol.standard.EchoService;
import com.baidu.brpc.protocol.standard.EchoServiceImpl;
import com.baidu.brpc.server.ServiceManager;
import com.baidu.brpc.utils.ByteBufUtils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

public class HttpProtoProtocolTest {

    private HttpRpcProtocol protocol = new HttpRpcProtocol(ProtocolType.PROTOCOL_HTTP_PROTOBUF_VALUE, "utf-8");

    @Test
    public void testEncodeHttpRequest() throws Exception {
        Request request = new HttpRequest();
        request.setTargetMethod(EchoService.class.getMethods()[0]);
        request.setArgs(new Object[] {Echo.EchoRequest.newBuilder().setMessage("hello").build()});
        request.setLogId(1L);
        request.setRpcMethodInfo(new ProtobufRpcMethodInfo(EchoService.class.getMethods()[0]));
        ByteBuf buf = protocol.encodeRequest(request);
        Assert.assertTrue(buf.readableBytes() > 0);
        System.out.println(buf.readableBytes());
        System.out.println(ByteBufUtils.byteBufToString(buf));
    }

    @Test
    public void testDecodeHttpRequest() throws Exception {
        ServiceManager serviceManager = ServiceManager.getInstance();
        serviceManager.registerService(new EchoServiceImpl(), null);

        ByteBuf content = Unpooled.wrappedBuffer(encodeBody(Echo.EchoRequest.newBuilder().setMessage("hello").build()));

        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.GET,
                "/example.EchoService/Echo", content);
        httpRequest.headers().set("log-id", 1);
        httpRequest.setUri("/example.EchoService/Echo");
        httpRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/proto; charset=utf-8");

        Request request = protocol.decodeRequest(httpRequest);

        assertEquals("example.EchoService", request.getRpcMethodInfo().getServiceName());
        assertEquals("Echo", request.getRpcMethodInfo().getMethodName());
        assertEquals(EchoService.class.getMethods()[0], request.getTargetMethod());
        assertEquals(EchoServiceImpl.class, request.getTarget().getClass());
    }

    @Test
    public void testEncodeHttpResponse() throws Exception {

        HttpRequest request = new HttpRequest();
        String contentType = "application/proto; charset=utf-8";
        request.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        request.headers().set(HttpHeaderNames.CONTENT_ENCODING, "utf-8");
        request.setRpcMethodInfo(new ProtobufRpcMethodInfo(EchoService.class.getMethods()[0]));
        Response response = new HttpResponse();
        response.setResult(Echo.EchoResponse.newBuilder().setMessage("hello").build());

        ProtobufRpcMethodInfo methodInfo = new ProtobufRpcMethodInfo(EchoService.class.getMethods()[0]);
        methodInfo.setTarget(new EchoServiceImpl());
        response.setRpcMethodInfo(methodInfo);

        protocol.encodeResponse(request, response);

    }

    public byte[] encodeBody(Object body) throws Exception {
        Method method = protocol.getClass().getDeclaredMethod("encodeBody", int.class, String.class,
                Object.class, RpcMethodInfo.class);
        method.setAccessible(true);
        Object r = method.invoke(protocol, ProtocolType.PROTOCOL_HTTP_PROTOBUF_VALUE, "utf-8",
                body, new ProtobufRpcMethodInfo(EchoService.class.getMethods()[0]));

        return (byte[]) r;
    }

}
