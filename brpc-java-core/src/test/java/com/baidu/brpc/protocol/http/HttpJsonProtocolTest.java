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

import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.protocol.Options.ProtocolType;
import com.baidu.brpc.protocol.http.json.HelloWorldService;
import com.baidu.brpc.protocol.http.json.HelloWorldServiceImpl;
import com.baidu.brpc.protocol.HttpRequest;
import com.baidu.brpc.protocol.HttpResponse;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import com.baidu.brpc.server.ServiceManager;
import com.baidu.brpc.utils.ByteBufUtils;
import com.google.gson.Gson;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

public class HttpJsonProtocolTest {

    private HttpRpcProtocol protocol = new HttpRpcProtocol(
            ProtocolType.PROTOCOL_HTTP_JSON_VALUE, "utf-8");

    @Test
    public void testEncodeHttpRequest() throws Exception {
        Request request = new HttpRequest();
        request.setTargetMethod(HelloWorldService.class.getMethods()[0]);
        request.setArgs(new Object[] {"hello"});
        request.setLogId(1L);
        ByteBuf buf = protocol.encodeRequest(request);
        Assert.assertTrue(buf.readableBytes() > 0);
        System.out.println(buf.readableBytes());
        System.out.println(ByteBufUtils.byteBufToString(buf));
    }

    @Test
    public void testDecodeHttpRequest() {
        ServiceManager serviceManager = ServiceManager.getInstance();
        serviceManager.registerService(new HelloWorldServiceImpl(), null);
        ByteBuf content = Unpooled.wrappedBuffer(new Gson().toJson("hello").getBytes());

        FullHttpRequest fullHttpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.GET,
                "/HelloWorldService/hello?k=v", content);
        fullHttpRequest.headers().set("log-id", 1);
        fullHttpRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=utf-8");

        Request request = protocol.decodeRequest(fullHttpRequest);

        assertEquals("HelloWorldService", request.getRpcMethodInfo().getServiceName());
        assertEquals("hello", request.getRpcMethodInfo().getMethodName());
        assertEquals(HelloWorldService.class.getMethods()[0], request.getTargetMethod());
        assertEquals(HelloWorldServiceImpl.class, request.getTarget().getClass());
    }

    @Test
    public void testEncodeHttpResponse() throws Exception {

        HttpRequest request = new HttpRequest();
        String contentType = "application/json; charset=utf-8";
        request.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        request.headers().set(HttpHeaderNames.CONTENT_ENCODING, "utf-8");
        Response response = new HttpResponse();
        response.setResult("hello world");
        protocol.encodeResponse(request, response);
//        FullHttpResponse fullHttpResponse = (FullHttpResponse) response.getMsg();
//
//        assertEquals(contentType, fullHttpResponse.headers().get(HttpHeaderNames.CONTENT_TYPE));
//        assertEquals(encodeBody(response.getResult()).length, fullHttpResponse.content().readableBytes());
    }

    public byte[] encodeBody(Object body) throws Exception {
        Method method = protocol.getClass().getDeclaredMethod("encodeBody", int.class, String.class,
                Object.class, RpcMethodInfo.class);
        method.setAccessible(true);
        Object r = method.invoke(protocol, ProtocolType.PROTOCOL_HTTP_JSON_VALUE, "utf-8", body,
                new RpcMethodInfo(HelloWorldService.class.getMethods()[0]));
        return (byte[]) r;
    }

}
