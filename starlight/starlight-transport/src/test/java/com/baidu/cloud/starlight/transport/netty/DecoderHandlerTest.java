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
 
package com.baidu.cloud.starlight.transport.netty;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.common.URI;
import com.baidu.cloud.starlight.api.exception.TransportException;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.model.RpcResponse;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.api.transport.channel.ChannelAttribute;
import com.baidu.cloud.starlight.api.transport.channel.ChannelSide;
import com.baidu.cloud.starlight.protocol.http.springrest.sse.SpringRestSseProtocol;
import com.baidu.cloud.starlight.transport.channel.LongRpcChannel;
import com.baidu.cloud.starlight.api.transport.channel.RpcChannel;
import com.baidu.cloud.starlight.protocol.brpc.BrpcEncoder;
import com.baidu.cloud.thirdparty.netty.buffer.ByteBuf;
import com.baidu.cloud.thirdparty.netty.buffer.CompositeByteBuf;
import com.baidu.cloud.thirdparty.netty.buffer.Unpooled;
import com.baidu.cloud.thirdparty.netty.channel.embedded.EmbeddedChannel;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.DefaultFullHttpResponse;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.DefaultHttpContent;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.DefaultHttpResponse;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpHeaderNames;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpObject;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpResponse;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpResponseEncoder;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpResponseStatus;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpVersion;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.LastHttpContent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by liuruisen on 2020/3/20.
 */
public class DecoderHandlerTest {

    private ByteBuf requestBuf = null;

    @Before
    public void init() {
        Map<String, Object> kvMap = new HashMap<>();
        kvMap.put(Constants.TRACE_ID_KEY, 123l);
        kvMap.put(Constants.SPAN_ID_KEY, 2l);
        kvMap.put(Constants.PARENT_SPAN_ID_KEY, 1l);
        kvMap.put("Key1", "Value1");

        BrpcEncoder brpcEncoder = new BrpcEncoder();

        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setMethodName("init");
        rpcRequest.setServiceClass(this.getClass());
        rpcRequest.setServiceConfig(new ServiceConfig());
        rpcRequest.setParams(new Object[] {"Test"});
        rpcRequest.setParamsTypes(new Class[] {String.class});
        rpcRequest.setProtocolName("brpc");
        rpcRequest.setAttachmentKv(kvMap);

        brpcEncoder.encodeBody(rpcRequest);
        requestBuf = brpcEncoder.encode(rpcRequest);
    }

    @Test
    public void decodeKnowProtocol() {
        EmbeddedChannel channel = new CustomEmbeddedChannel("localhost", 8006, new DecoderHandler());

        // none rpcChannel attr in Channel
        try {
            channel.writeInbound(requestBuf.retain());
        } catch (Exception e) {
            Assert.assertTrue(e instanceof TransportException);
        }

        // set know protocol rpcChannel
        URI.Builder builder = new URI.Builder("brpc", "localhost", 8006);
        LongRpcChannel rpcChannel = new LongRpcChannel(channel, ChannelSide.SERVER);
        ChannelAttribute attribute = new ChannelAttribute(rpcChannel);
        channel.attr(RpcChannel.ATTRIBUTE_KEY).set(attribute);
        // know protocol decode
        channel.writeInbound(requestBuf.retain());
        RpcRequest rpcRequest = channel.readInbound();
        Assert.assertTrue(rpcRequest.getServiceName().equals(this.getClass().getName()));
    }

    @Test
    public void decodeUnKnowProtocol() {
        EmbeddedChannel channel = new CustomEmbeddedChannel("localhost", 8006, new DecoderHandler());

        // set unKnow protocol rpcChannel
        URI.Builder builder2 = new URI.Builder(Constants.UNSPECIFIED_PROTOCOL, "localhost", 8006);
        LongRpcChannel rpcChannel2 = new LongRpcChannel(channel, ChannelSide.SERVER);
        ChannelAttribute attribute = new ChannelAttribute(rpcChannel2);
        channel.attr(RpcChannel.ATTRIBUTE_KEY).set(attribute);
        // unKnow protocol rpcChannel
        channel.writeInbound(requestBuf.retain());
        RpcRequest rpcRequest1 = channel.readInbound();
        Assert.assertTrue(rpcRequest1.getServiceName().equals(this.getClass().getName()));
        Assert.assertTrue(attribute.getChannelProtocol().equals("brpc"));
    }

    @Test
    public void decodeUnCorrectProtocol() {
        EmbeddedChannel channel = new CustomEmbeddedChannel("localhost", 8006, new DecoderHandler());

        // set un correct, request is brpc but uri protocol is atest
        URI.Builder builder2 = new URI.Builder("atest", "localhost", 8006);
        LongRpcChannel rpcChannel2 = new LongRpcChannel(channel, ChannelSide.SERVER);
        ChannelAttribute attribute = new ChannelAttribute(rpcChannel2);
        channel.attr(RpcChannel.ATTRIBUTE_KEY).set(attribute);
        // unKnow protocol
        channel.writeInbound(requestBuf.retain());
        RpcRequest rpcRequest = channel.readInbound();
        Assert.assertTrue(rpcRequest.getServiceName().equals(this.getClass().getName()));
        Assert.assertTrue(attribute.getChannelProtocol().equals("brpc"));
    }

    @Test
    public void decodeUnSpecificProtocol() {
        EmbeddedChannel channel = new CustomEmbeddedChannel("localhost", 8006, new DecoderHandler());

        // set unKnow protocol rpcChannel
        URI.Builder builder2 = new URI.Builder(Constants.UNSPECIFIED_PROTOCOL, "localhost", 8006);
        LongRpcChannel rpcChannel2 = new LongRpcChannel(channel, ChannelSide.SERVER);
        ChannelAttribute attribute = new ChannelAttribute(rpcChannel2);
        channel.attr(RpcChannel.ATTRIBUTE_KEY).set(attribute);
        // unKnow protocol rpcChannel
        channel.writeInbound(requestBuf.retain());
        RpcRequest rpcRequest = channel.readInbound();
        Assert.assertTrue(rpcRequest.getServiceName().equals(this.getClass().getName()));
        Assert.assertTrue(attribute.getChannelProtocol().equals("brpc"));
    }

    @Test
    public void decodeErrorMsg() {
        EmbeddedChannel channel = new CustomEmbeddedChannel("localhost", 8006, new DecoderHandler());

        URI.Builder builder2 = new URI.Builder(Constants.UNSPECIFIED_PROTOCOL, "localhost", 8006);
        LongRpcChannel rpcChannel2 = new LongRpcChannel(channel, ChannelSide.SERVER);
        channel.attr(RpcChannel.ATTRIBUTE_KEY).set(new ChannelAttribute(rpcChannel2));
        try {
            // InSufficient msg
            ByteBuf byteBuf = Unpooled.wrappedBuffer("1".getBytes());
            channel.writeInbound(byteBuf);
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }

        try {
            // error msg
            ByteBuf byteBuf = Unpooled.wrappedBuffer("12345678901234567890".getBytes());
            channel.writeInbound(byteBuf);
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }

    @Test
    public void testDecodeHttpProtocol() {

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

        int half = outboundByteBuf.readableBytes() / 2;
        // 前半个
        ByteBuf halfOutboundByteBuf = outboundByteBuf.slice(0, half);
        // 后半个
        ByteBuf restOutboundByteBuf = outboundByteBuf.slice(half, outboundByteBuf.readableBytes() - half);
        // -- 模拟完毕

        EmbeddedChannel channel = new CustomEmbeddedChannel("localhost", 8006, new DecoderHandler());
        LongRpcChannel rpcChannel = new LongRpcChannel(channel, ChannelSide.SERVER);
        channel.attr(RpcChannel.ATTRIBUTE_KEY).set(new ChannelAttribute(rpcChannel));

        try {
            // 收到一个包被拆包的情况，第一次就会失败
            channel.writeInbound(halfOutboundByteBuf);
        } catch (Exception e) {
            System.out.println("ignore1:" + e.getMessage());
        }

        // 收到第二个包的时候，包就完整了，就不会报错了
        channel.writeInbound(restOutboundByteBuf);

        // 最终能 decode 出一个完整的Response包
        RpcResponse rpcResponse = channel.readInbound();
        Assert.assertNotNull(rpcResponse);

    }

    @Test
    public void testDecodeSSEResponse() {

        // 模拟多个sse response数据包
        EmbeddedChannel outboundChannel = new EmbeddedChannel(new HttpResponseEncoder());

        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/event-stream");
        response.headers().set(HttpHeaderNames.CACHE_CONTROL, "no-cache");
        response.headers().set(HttpHeaderNames.CONNECTION, "keep-alive");
        response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, "chunked");

        outboundChannel.writeOutbound(response);

        for (int i = 0; i < 5; i++) {
            String eventData = "data: Event from server\n\n";

            ByteBuf content = Unpooled.copiedBuffer(eventData, StandardCharsets.UTF_8);
            DefaultHttpContent httpContent = new DefaultHttpContent(content);
            outboundChannel.writeOutbound(httpContent);
        }

        outboundChannel.writeOutbound(LastHttpContent.EMPTY_LAST_CONTENT);

        CompositeByteBuf outboundByteBuf = Unpooled.compositeBuffer();
        for (Object msg : outboundChannel.outboundMessages()) {
            outboundByteBuf.addComponent(true, (ByteBuf) msg);
        }

        // 把数据分为 多份
        int count = 5;

        if (outboundByteBuf.readableBytes() < count) {
            // 如果不够分 ，则不能进行这个测试
            System.out.println("[WARN] data not enough");
            return;
        }

        int size_per_count = outboundByteBuf.readableBytes() / count;

        List<ByteBuf> byteBufList = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            if (i == count - 1) {
                // 最后一份 取剩下所有的
                byteBufList.add(
                    outboundByteBuf.copy(i * size_per_count, outboundByteBuf.readableBytes() - i * size_per_count));
            } else {
                byteBufList.add(outboundByteBuf.copy(i * size_per_count, size_per_count));
            }
        }
        // -- 模拟完毕

        EmbeddedChannel channel = new CustomEmbeddedChannel("localhost", 8006, new DecoderHandler());
        LongRpcChannel rpcChannel = new LongRpcChannel(channel, ChannelSide.CLIENT);
        ChannelAttribute channelAttribute = new ChannelAttribute(rpcChannel);
        channel.attr(RpcChannel.ATTRIBUTE_KEY).set(channelAttribute);
        channelAttribute.resetChannelProtocol(SpringRestSseProtocol.PROTOCOL_NAME);

        for (ByteBuf partByteBuf : byteBufList) {
            try {
                channel.writeInbound(partByteBuf);
            } catch (Exception e) {
                System.out.println("ignore:" + e.getMessage());
            }
        }

        List<Response> messages =
            channel.inboundMessages().stream().map(o -> (Response) o).collect(Collectors.toList());

        List<HttpObject> httpObjects = messages.stream().flatMap(resp -> ((List<HttpObject>) resp.getResult()).stream())
            .collect(Collectors.toList());

        Assert.assertEquals(httpObjects.size(), count + 2);
    }

    @Test
    public void testDecodeSSEResponse2() {

        // 模拟多个sse response数据包 & 错误请求
        EmbeddedChannel outboundChannel = new EmbeddedChannel(new HttpResponseEncoder());

        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.SERVICE_UNAVAILABLE);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/event-stream");
        response.headers().set(HttpHeaderNames.CACHE_CONTROL, "no-cache");
        response.headers().set(HttpHeaderNames.CONNECTION, "keep-alive");
        response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, "chunked");

        outboundChannel.writeOutbound(response);

        for (int i = 0; i < 5; i++) {
            String eventData = "data: Event from server\n\n";

            ByteBuf content = Unpooled.copiedBuffer(eventData, StandardCharsets.UTF_8);
            DefaultHttpContent httpContent = new DefaultHttpContent(content);
            outboundChannel.writeOutbound(httpContent);
        }

        outboundChannel.writeOutbound(LastHttpContent.EMPTY_LAST_CONTENT);

        CompositeByteBuf outboundByteBuf = Unpooled.compositeBuffer();
        for (Object msg : outboundChannel.outboundMessages()) {
            outboundByteBuf.addComponent(true, (ByteBuf) msg);
        }

        // 把数据分为 多份
        int count = 5;

        if (outboundByteBuf.readableBytes() < count) {
            // 如果不够分 ，则不能进行这个测试
            System.out.println("[WARN] data not enough");
            return;
        }

        int size_per_count = outboundByteBuf.readableBytes() / count;

        List<ByteBuf> byteBufList = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            if (i == count - 1) {
                // 最后一份 取剩下所有的
                byteBufList.add(
                    outboundByteBuf.copy(i * size_per_count, outboundByteBuf.readableBytes() - i * size_per_count));
            } else {
                byteBufList.add(outboundByteBuf.copy(i * size_per_count, size_per_count));
            }
        }
        // -- 模拟完毕

        EmbeddedChannel channel = new CustomEmbeddedChannel("localhost", 8006, new DecoderHandler());
        LongRpcChannel rpcChannel = new LongRpcChannel(channel, ChannelSide.CLIENT);
        ChannelAttribute channelAttribute = new ChannelAttribute(rpcChannel);
        channel.attr(RpcChannel.ATTRIBUTE_KEY).set(channelAttribute);
        channelAttribute.resetChannelProtocol(SpringRestSseProtocol.PROTOCOL_NAME);

        for (ByteBuf partByteBuf : byteBufList) {
            try {
                channel.writeInbound(partByteBuf);
            } catch (Exception e) {
                System.out.println("ignore:" + e.getMessage());
            }
        }

        List<Response> messages =
            channel.inboundMessages().stream().map(o -> (Response) o).collect(Collectors.toList());

        List<HttpObject> httpObjects = messages.stream().flatMap(resp -> ((List<HttpObject>) resp.getResult()).stream())
            .collect(Collectors.toList());

        Assert.assertEquals(httpObjects.size(), 1);
    }

}