/*
 * Copyright (c) 2019 Baidu, Inc. All Rights Reserved.
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
 
package com.baidu.cloud.starlight.protocol.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import com.baidu.cloud.starlight.api.exception.CodecException;
import com.baidu.cloud.starlight.api.model.MsgBase;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.model.RpcResponse;
import com.baidu.cloud.starlight.api.transport.buffer.DynamicCompositeByteBuf;
import com.baidu.cloud.starlight.serialization.serializer.JsonSerializer;
import org.junit.Test;

import static com.baidu.cloud.starlight.api.exception.CodecException.BODY_DECODE_EXCEPTION;
import static com.baidu.cloud.starlight.api.exception.CodecException.PROTOCOL_DECODE_EXCEPTION;
import static com.baidu.cloud.starlight.api.exception.CodecException.PROTOCOL_INSUFFICIENT_DATA_EXCEPTION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by liuruisen on 2020/5/28.
 */
public class HttpDecoderTest {

    private HttpDecoder decoder = new HttpDecoder() {
        @Override
        protected Request reverseConvertRequest(FullHttpRequest httpRequest) {
            return null;
        }
    };
    private HttpEncoder encoder = new HttpEncoder() {
        @Override
        protected FullHttpRequest convertRequest(Request rpcRequest) {
            return null;
        }
    };

    @Test
    public void decodeRequest() {
        // decode request
        FullHttpRequest httpRequest =
            new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/calculate?a=10&b=5");
        httpRequest.headers().add("Operator", "Add");
        httpRequest.headers().add(AbstractHttpProtocol.X_STARLIGHT_ID, "1");

        Request request = new RpcRequest();
        request.setParams(new Object[] {httpRequest});
        ByteBuf byteBuf = encoder.encode(request);

        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf();
        compositeByteBuf.addBuffer(byteBuf);
        MsgBase msgBase = decoder.decode(compositeByteBuf);
        assertNull(msgBase);
    }

    @Test
    public void decodeResponse() {
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf();
        // decode response
        JsonSerializer serializer = new JsonSerializer();
        User user = new User();
        user.setUserId(1);
        user.setName("312313");
        ByteBuf content = Unpooled.wrappedBuffer(serializer.serialize(user, User.class));

        // normal response
        FullHttpResponse httpResponse =
            new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
        httpResponse.headers().add(AbstractHttpProtocol.X_STARLIGHT_ID, "1");
        httpResponse.headers().add(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
        httpResponse.headers().add(HttpHeaderNames.CONTENT_TYPE, "application/json");

        Response response = new RpcResponse();
        response.setResult(httpResponse);
        ByteBuf byteBuf2 = encoder.encode(response);
        compositeByteBuf.addBuffer(byteBuf2);
        MsgBase msgBase2 = decoder.decode(compositeByteBuf);
        assertTrue(msgBase2 != null);
        assertTrue(msgBase2.getId() == 1);
        assertTrue(msgBase2 instanceof Response);
        assertTrue(((Response) msgBase2).getStatus() == HttpResponseStatus.OK.code());
    }

    @Test
    public void decodeErrorResponse() {
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf();
        // abnormal response
        FullHttpResponse errorResponse =
            new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        errorResponse.headers().add(AbstractHttpProtocol.X_STARLIGHT_ID, "1");
        errorResponse.headers().add(HttpHeaderNames.CONTENT_LENGTH, 0);
        // errorResponse.headers().add(HttpHeaderNames.CONTENT_TYPE, "application/json");

        Response response1 = new RpcResponse();
        response1.setResult(errorResponse);
        ByteBuf byteBuf = encoder.encode(response1);
        compositeByteBuf.addBuffer(byteBuf);
        MsgBase msgBase = decoder.decode(compositeByteBuf);
        assertTrue(msgBase instanceof Response);
        assertTrue(((Response) msgBase).getStatus() == HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
    }

    @Test
    public void decodeNull() {
        // input bytebuf is null
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf();
        try {
            decoder.decode(compositeByteBuf);
        } catch (CodecException e) {
            assertEquals(e.getCode(), PROTOCOL_INSUFFICIENT_DATA_EXCEPTION);
        }

        // request id is null
        FullHttpRequest httpRequest =
            new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/calculate?a=10&b=5");
        httpRequest.headers().add("Operator", "Add");
        Request request = new RpcRequest();
        request.setParams(new Object[] {httpRequest});
        ByteBuf byteBuf = encoder.encode(request);
        compositeByteBuf.addBuffer(byteBuf);
        decoder.decode(compositeByteBuf);
        assertEquals(0, compositeByteBuf.readableBytes());

        // content-type is not support
        FullHttpRequest httpRequest1 =
            new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/calculate?a=10&b=5");
        httpRequest1.headers().add("Operator", "Add");
        httpRequest1.headers().add(HttpHeaderNames.CONTENT_TYPE, "test/test");
        httpRequest1.headers().add(AbstractHttpProtocol.X_STARLIGHT_ID, "1");
        Request request1 = new RpcRequest();
        request1.setParams(new Object[] {httpRequest1});
        compositeByteBuf.addBuffer(encoder.encode(request1));
        decoder.decode(compositeByteBuf);

        assertEquals(compositeByteBuf.readableBytes(), 0);
    }

    @Test
    public void decodeRequestBody() {
        User user = new User();
        user.setName("name");
        user.setUserId(123);
        byte[] bytes = new JsonSerializer().serialize(user, User.class);

        // decode request with body bytes
        Request request = new RpcRequest(1);
        request.setProtocolName("springrest");
        request.setParamsTypes(new Class<?>[] {User.class});
        request.setParams(new Object[] {bytes});
        request.setGenericParamsTypes(new Class<?>[] {User.class});
        decoder.decodeBody(request);
        assertTrue(request.getParams().length == 1);
        assertEquals(((User) request.getParams()[0]).getName(), "name");

        // decode request without params
        Request request2 = new RpcRequest(1);
        request2.setProtocolName("springrest");
        decoder.decodeBody(request2);
        assertTrue(request2.getParams() == null);

        // decode request without bytes params
        Request request3 = new RpcRequest(1);
        request3.setProtocolName("springrest");
        request3.setParamsTypes(new Class<?>[] {String.class});
        request3.setParams(new Object[] {"test"});
        decoder.decodeBody(request3);
        assertTrue(request3.getParams().length == 1);
        assertEquals(((String) request3.getParams()[0]), "test");
    }

    @Test
    public void decodeResponseBody() {
        User user = new User();
        user.setName("name");
        user.setUserId(123);
        byte[] bytes = new JsonSerializer().serialize(user, User.class);

        // decode response body, null body byets
        Response response = new RpcResponse(1);
        response.setProtocolName("springrest");
        decoder.decodeBody(response);

        // decode response body, error body bytes
        response.setStatus(500);
        response.setBodyBytes(new JsonSerializer().serialize("Error", String.class));
        decoder.decodeBody(response);
        assertTrue(response.getErrorMsg().equals("Error"));

        // decode response body, Success
        response.setStatus(200);
        response.setBodyBytes(bytes);
        response.setReturnType(User.class);
        response.setGenericReturnType(User.class);
        decoder.decodeBody(response);
        assertTrue(response.getResult() != null);
        assertTrue(response.getResult() instanceof User);
    }

    @Test
    public void decodeResponseBodyError() {

        // decode null
        try {
            decoder.decodeBody(null);
        } catch (CodecException e) {
            assertEquals(e.getCode(), BODY_DECODE_EXCEPTION);
        }

        // decode without ReturnType
        User user = new User();
        user.setName("name");
        user.setUserId(123);
        byte[] bytes = new JsonSerializer().serialize(user, User.class);
        Response response = new RpcResponse(1);
        response.setProtocolName("springrest");
        response.setStatus(200);
        response.setBodyBytes(bytes);
        try {
            decoder.decodeBody(response);
        } catch (CodecException e) {
            assertEquals(e.getCode(), BODY_DECODE_EXCEPTION);
        }

    }
}