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
import com.baidu.cloud.starlight.api.model.AbstractMsgBase;
import com.baidu.cloud.starlight.api.model.MsgBase;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.api.transport.channel.ChannelAttribute;
import com.baidu.cloud.starlight.api.transport.channel.ChannelSide;
import com.baidu.cloud.starlight.api.transport.channel.RpcChannel;
import com.baidu.cloud.starlight.transport.channel.LongRpcChannel;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by liuruisen on 2020/3/20.
 */
public class EncoderHandlerTest {

    private RpcRequest rpcRequest;

    @Before
    public void init() {
        Map<String, Object> kvMap = new HashMap<>();
        kvMap.put(Constants.TRACE_ID_KEY, 123l);
        kvMap.put(Constants.SPAN_ID_KEY, 2l);
        kvMap.put(Constants.PARENT_SPAN_ID_KEY, 1l);
        kvMap.put("Key1", "Value1");
        rpcRequest = new RpcRequest();
        rpcRequest.setMethodName("init");
        rpcRequest.setServiceClass(this.getClass());
        rpcRequest.setServiceConfig(new ServiceConfig());
        rpcRequest.setParams(new Object[] {"Test"});
        rpcRequest.setParamsTypes(new Class[] {String.class});
        rpcRequest.setProtocolName("brpc");
        rpcRequest.setAttachmentKv(kvMap);
    }

    @Test
    public void encode() {
        EmbeddedChannel channel = new EmbeddedChannel(new EncoderHandler());
        LongRpcChannel rpcChannel = new LongRpcChannel(channel, ChannelSide.CLIENT);
        ChannelAttribute attribute = new ChannelAttribute(rpcChannel);
        channel.attr(RpcChannel.ATTRIBUTE_KEY).set(attribute);

        channel.writeOutbound(rpcRequest);
        ByteBuf byteBuf = channel.readOutbound();
        Assert.assertTrue(byteBuf.readableBytes() > 0);
    }

    @Test(expected = Exception.class)
    public void encodeErrorMsg() {
        EmbeddedChannel channel = new EmbeddedChannel(new EncoderHandler());

        // encode null protocol
        rpcRequest.setProtocolName(null);
        channel.writeOutbound(rpcRequest);

        // encode null protocol
        MsgBase msgBase = new AbstractMsgBase() {};
        msgBase.setProtocolName("brpc");
        channel.writeOutbound(msgBase);
    }

}