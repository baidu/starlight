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
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import com.baidu.cloud.starlight.api.exception.CodecException;
import com.baidu.cloud.starlight.api.model.AbstractMsgBase;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.model.RpcResponse;
import com.baidu.cloud.starlight.serialization.serializer.JsonSerializer;
import org.junit.Test;

import java.util.Collections;

import static com.baidu.cloud.starlight.api.exception.CodecException.BODY_ENCODE_EXCEPTION;
import static com.baidu.cloud.starlight.api.exception.CodecException.PROTOCOL_ENCODE_EXCEPTION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by liuruisen on 2020/5/28.
 */
public class HttpEncoderTest {

    private HttpEncoder httpEncoder = new HttpEncoder() {
        @Override
        protected FullHttpRequest convertRequest(Request rpcRequest) {
            return null;
        }
    };

    @Test
    public void encode() {
        // encode request
        FullHttpRequest httpRequest =
            new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/calculate?a=10&b=5");
        httpRequest.headers().add("Operator", "Add");

        Request request = new RpcRequest();
        request.setParams(new Object[] {httpRequest});
        ByteBuf byteBuf = httpEncoder.encode(request);

        assertTrue(byteBuf != null);
        assertTrue(byteBuf.readableBytes() > 0);

        // encode response
        FullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        Response response = new RpcResponse();
        response.setResult(httpResponse);
        ByteBuf byteBuf2 = httpEncoder.encode(response);
        assertTrue(byteBuf2 != null);
        assertTrue(byteBuf2.readableBytes() > 0);
    }

    @Test
    public void echoNull() {
        // input == null
        try {
            httpEncoder.encode(null);
        } catch (CodecException e) {
            assertEquals(e.getCode(), PROTOCOL_ENCODE_EXCEPTION);
        }

        // request params null
        try {
            httpEncoder.encode(new RpcRequest());
        } catch (CodecException e) {
            assertEquals(e.getCode(), PROTOCOL_ENCODE_EXCEPTION);
        }

        // response params null
        try {
            httpEncoder.encode(new RpcResponse());
        } catch (CodecException e) {
            assertEquals(e.getCode(), PROTOCOL_ENCODE_EXCEPTION);
        }

        // msgBase neither Request nor Response
        try {
            httpEncoder.encode(new AbstractMsgBase() {
                @Override
                public long getId() {
                    return super.getId();
                }
            });
        } catch (CodecException e) {
            assertEquals(e.getCode(), PROTOCOL_ENCODE_EXCEPTION);
        }
    }

    @Test
    public void encodeBody() {
        // encode requestBody
        Request request = new RpcRequest();
        httpEncoder.encodeBody(request);
        assertEquals(request.getParams().length, 1);
        assertNull(request.getParams()[0]);

        // encode response body -- error
        Response response = new RpcResponse(123);
        response.setProtocolName("springrest");
        response.setStatus(500);
        response.setErrorMsg("Error 500");
        response.setAttachmentKv(Collections.singletonMap("test", 123));
        httpEncoder.encodeBody(response);
        assertNotNull(response.getResult());
        assertTrue(response.getResult() instanceof FullHttpResponse);
        FullHttpResponse httpResponse = (FullHttpResponse) response.getResult();
        assertTrue(httpResponse.content().readableBytes() > 0);
        JsonSerializer jsonSerializer = new JsonSerializer();
        byte[] errorBytes = jsonSerializer.serialize("Error 500", String.class);
        assertTrue(httpResponse.content().readableBytes() == errorBytes.length);

        // encode response body -- success
        Response response1 = new RpcResponse(123);
        response1.setProtocolName("springrest");
        response1.setStatus(200);
        response1.setResult("Success 200");
        response1.setRequest(request);
        httpEncoder.encodeBody(response1);
        assertNotNull(response1.getResult());
        assertTrue(response1.getResult() instanceof FullHttpResponse);
        FullHttpResponse httpResponse1 = (FullHttpResponse) response1.getResult();
        assertTrue(httpResponse1.content().readableBytes() > 0);
        byte[] responseBytes = new byte[httpResponse1.content().readableBytes()];
        httpResponse1.content().readBytes(responseBytes);
        String resultStr = (String) new JsonSerializer().deserialize(responseBytes, String.class);
        assertEquals(resultStr, "Success 200");
    }

    @Test
    public void encodeBodyNull() {
        try {
            httpEncoder.encodeBody(null);
        } catch (CodecException e) {
            assertEquals(e.getCode(), BODY_ENCODE_EXCEPTION);
        }
    }
}