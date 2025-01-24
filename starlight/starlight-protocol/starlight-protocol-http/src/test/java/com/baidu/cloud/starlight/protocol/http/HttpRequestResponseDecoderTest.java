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

import com.baidu.cloud.thirdparty.netty.buffer.ByteBuf;
import com.baidu.cloud.thirdparty.netty.buffer.CompositeByteBuf;
import com.baidu.cloud.thirdparty.netty.buffer.Unpooled;
import com.baidu.cloud.thirdparty.netty.buffer.UnpooledByteBufAllocator;
import com.baidu.cloud.thirdparty.netty.channel.embedded.EmbeddedChannel;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.DefaultFullHttpResponse;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.DefaultHttpContent;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.DefaultHttpRequest;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.DefaultHttpResponse;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpHeaderValues;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpMessage;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpRequest;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpResponse;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpResponseEncoder;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpResponseStatus;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpVersion;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Queue;

/**
 * Created by liuruisen on 2020/6/2.
 */
public class HttpRequestResponseDecoderTest {

    private HttpRequestResponseDecoder requestResponseDecoder = new HttpRequestResponseDecoder();

    @Test
    public void createMessage() throws Exception {
        String[] responseInitLines = new String[] {"HTTP/1.1", "200", "OK"};
        HttpMessage httpMessage = requestResponseDecoder.createMessage(responseInitLines);

        Assert.assertTrue(httpMessage instanceof HttpResponse);
        Assert.assertTrue(((DefaultHttpResponse) httpMessage).status().code() == 200);
        Assert.assertFalse(requestResponseDecoder.isDecodingRequest());

        String[] requestInitLines = new String[] {"GET", "/echo", "HTTP/1.1"};
        HttpMessage httpMessage2 = requestResponseDecoder.createMessage(requestInitLines);
        Assert.assertTrue(httpMessage2 instanceof HttpRequest);
        Assert.assertTrue(((DefaultHttpRequest) httpMessage2).method().name().equals("GET"));
        Assert.assertTrue(requestResponseDecoder.isDecodingRequest());
    }

    @Test
    public void createInvalidMessage() throws Exception {
        // response
        String[] responseInitLines = new String[] {"HTTP/1.1", "200", "OK"};
        requestResponseDecoder.createMessage(responseInitLines);

        HttpMessage httpMessage = requestResponseDecoder.createInvalidMessage();
        Assert.assertTrue(httpMessage instanceof HttpResponse);
        Assert.assertTrue(((DefaultHttpResponse) httpMessage).status().code() == 999);

        // request
        String[] requestInitLines = new String[] {"GET", "/echo", "HTTP/1.1"};
        requestResponseDecoder.createMessage(requestInitLines);

        HttpMessage httpMessage2 = requestResponseDecoder.createInvalidMessage();
        Assert.assertTrue(httpMessage2 instanceof HttpRequest);
        Assert.assertTrue(((DefaultHttpRequest) httpMessage2).method().name().equals("GET"));
    }

    @Test
    public void testDecodeResponse1() {
        // 模拟两个response数据包 粘在一起
        ByteBuf data1 = Unpooled.wrappedBuffer("response1".getBytes(StandardCharsets.UTF_8));
        DefaultFullHttpResponse response1 =
            new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, data1);
        response1.headers().add("Content-Length", data1.readableBytes());

        ByteBuf data2 = Unpooled.wrappedBuffer("response1".getBytes(StandardCharsets.UTF_8));
        DefaultFullHttpResponse response2 =
            new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, data2);
        response2.headers().add("Content-Length", data2.readableBytes());

        EmbeddedChannel outboundChannel = new EmbeddedChannel(new HttpResponseEncoder());
        outboundChannel.writeOutbound(response1, response2);

        CompositeByteBuf outboundByteBuf = Unpooled.compositeBuffer();
        for (Object msg : outboundChannel.outboundMessages()) {
            outboundByteBuf.addComponent(true, (ByteBuf) msg);
        }
        // -- 模拟完毕

        EmbeddedChannel inboundChannel = new EmbeddedChannel(new HttpRequestResponseDecoder());

        inboundChannel.writeInbound(outboundByteBuf);
        Queue<Object> inboundMessages = inboundChannel.inboundMessages();
        Assert.assertTrue(inboundMessages.size() == 1);

        inboundChannel.writeInbound(outboundByteBuf);
        inboundMessages = inboundChannel.inboundMessages();
        Assert.assertTrue(inboundMessages.size() == 2);
    }

    @Test
    public void testDecodeResponse2() {
        // 模拟 半个response数据包
        ByteBuf data1 = Unpooled.wrappedBuffer("response1".getBytes(StandardCharsets.UTF_8));
        DefaultFullHttpResponse response1 =
            new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, data1);
        response1.headers().add("Content-Length", data1.readableBytes());

        EmbeddedChannel outboundChannel = new EmbeddedChannel(new HttpResponseEncoder());
        outboundChannel.writeOutbound(response1);

        CompositeByteBuf outboundByteBuf = Unpooled.compositeBuffer();
        for (Object msg : outboundChannel.outboundMessages()) {
            outboundByteBuf.addComponent(true, (ByteBuf) msg);
        }

        ByteBuf halfOutboundByteBuf = outboundByteBuf.slice(0, outboundByteBuf.readableBytes() / 2);
        // -- 模拟完毕

        EmbeddedChannel inboundChannel = new EmbeddedChannel(new HttpRequestResponseDecoder());

        inboundChannel.writeInbound(halfOutboundByteBuf);
        Queue<Object> inboundMessages = inboundChannel.inboundMessages();
        // 半个数据包读不出东西
        Assert.assertTrue(inboundMessages.size() == 0);
    }
}