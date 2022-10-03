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
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.api.transport.channel.ChannelAttribute;
import com.baidu.cloud.starlight.api.transport.channel.ChannelSide;
import com.baidu.cloud.starlight.transport.channel.LongRpcChannel;
import com.baidu.cloud.starlight.api.transport.channel.RpcChannel;
import com.baidu.cloud.starlight.protocol.brpc.BrpcEncoder;
import com.baidu.cloud.thirdparty.netty.buffer.ByteBuf;
import com.baidu.cloud.thirdparty.netty.buffer.Unpooled;
import com.baidu.cloud.thirdparty.netty.channel.embedded.EmbeddedChannel;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

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

}