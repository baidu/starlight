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

import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.protocol.Options.ProtocolType;
import com.baidu.brpc.protocol.RpcRequest;
import com.baidu.brpc.protocol.RpcResponse;
import com.baidu.brpc.protocol.http.json.HelloWorldService;
import com.baidu.brpc.protocol.http.json.HelloWorldServiceImpl;
import com.baidu.brpc.server.ServiceManager;
import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

public class HttpJsonProtocolTest {

    private HttpRpcProtocol protocol = new HttpRpcProtocol(ProtocolType.PROTOCOL_HTTP_JSON_VALUE, "utf-8");

    @Test
    public void testEncodeHttpRequest() throws Exception {
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setTargetMethod(HelloWorldService.class.getMethods()[0]);
        rpcRequest.setArgs(new Object[] {"hello"});
        rpcRequest.setLogId(1L);

        FullHttpRequest response = protocol.encodeHttpRequest(rpcRequest);

        assertEquals("/HelloWorldService/hello", response.getUri());
        assertEquals(new Gson().toJson("hello").length(), response.content().readableBytes());
        assertEquals(1, response.headers().getInt("log-id").intValue());
        assertEquals("application/json; charset=utf-8",
                response.headers().getAsString(HttpHeaderNames.CONTENT_TYPE));
    }


    @Test
    public void testDecodeHttpRequest() {
        ServiceManager serviceManager = ServiceManager.getInstance();
        serviceManager.registerService(new HelloWorldServiceImpl());

        ByteBuf content = Unpooled.wrappedBuffer(new Gson().toJson("hello").getBytes());

        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.GET,
                "/HelloWorldService/hello", content);
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.headers().set("log-id", 1);
        rpcRequest.setUri("/HelloWorldService/hello");
        rpcRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=utf-8");

        protocol.decodeHttpRequest(httpRequest, rpcRequest);

        assertEquals("HelloWorldService", rpcRequest.getRpcMethodInfo().getServiceName());
        assertEquals("hello", rpcRequest.getRpcMethodInfo().getMethodName());
        assertEquals(HelloWorldService.class.getMethods()[0], rpcRequest.getTargetMethod());
        assertEquals(HelloWorldServiceImpl.class, rpcRequest.getTarget().getClass());
    }

    @Test
    public void testEncodeHttpResponse() throws Exception {

        RpcRequest rpcRequest = new RpcRequest();
        String contentType = "application/json; charset=utf-8";
        rpcRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        rpcRequest.headers().set(HttpHeaderNames.CONTENT_ENCODING, "utf-8");
        RpcResponse rpcResponse = new RpcResponse();
        rpcResponse.setResult("hello world");

        FullHttpResponse response = protocol.encodeHttpResponse(rpcRequest, rpcResponse);

        assertEquals(contentType, response.headers().get(HttpHeaderNames.CONTENT_TYPE));
        assertEquals(encodeBody(rpcResponse.getResult()).length, response.content().readableBytes());
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
