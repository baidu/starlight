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
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.model.RpcResponse;
import com.baidu.cloud.starlight.api.transport.channel.ChannelAttribute;
import com.baidu.cloud.starlight.api.transport.channel.ChannelSide;
import com.baidu.cloud.starlight.transport.channel.LongRpcChannel;
import com.baidu.cloud.starlight.api.transport.channel.RpcChannel;
import com.baidu.cloud.starlight.transport.protocol.test.ATestProtocol;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

/**
 * Created by liuruisen on 2020/3/23.
 */
public class HeartbeatHandlerTest {

    /**
     * Test heartbeat trigger when Protocol not support heartbeat msg
     * 
     * @throws Exception
     */
    @Test
    public void clientHeartbeatEventProtocolNotSupport() throws Exception {
        IdleStateHandler idleStateHandler = new IdleStateHandler(1L, 0L, 0L, TimeUnit.SECONDS);

        EmbeddedChannel channel = new EmbeddedChannel(idleStateHandler, new HeartbeatHandler());
        LongRpcChannel rpcChannel = Mockito.mock(LongRpcChannel.class);
        doNothing().when(rpcChannel).reconnect();
        doReturn(channel).when(rpcChannel).channel();
        doReturn(ChannelSide.CLIENT).when(rpcChannel).side();
        doReturn("brpc").when(rpcChannel).getAttribute(Constants.PROTOCOL_ATTR_KEY);
        ChannelAttribute attribute = new ChannelAttribute(rpcChannel);
        channel.attr(RpcChannel.ATTRIBUTE_KEY).set(attribute);

        TimeUnit.SECONDS.sleep(2);
        channel.runPendingTasks();

        Assert.assertTrue(channel.attr(HeartbeatHandler.HEARTBEAT_FAIL_TIMES).get() > 0);

        channel.finish();
    }

    /**
     * Test heartbeat trigger when Protocol support heartbeat msg
     * 
     * @throws Exception
     */
    @Test
    public void clientHeartbeatEventProtocolSupport() throws Exception {
        IdleStateHandler idleStateHandler = new IdleStateHandler(1L, 0L, 0L, TimeUnit.SECONDS);
        EmbeddedChannel channel = new EmbeddedChannel(idleStateHandler, new HeartbeatHandler());

        LongRpcChannel rpcChannel = Mockito.mock(LongRpcChannel.class);
        doNothing().when(rpcChannel).reconnect();
        doReturn(channel).when(rpcChannel).channel();
        doReturn(ChannelSide.CLIENT).when(rpcChannel).side();
        ChannelAttribute attribute = new ChannelAttribute(rpcChannel);
        channel.attr(RpcChannel.ATTRIBUTE_KEY).set(attribute);

        TimeUnit.SECONDS.sleep(2);
        channel.runPendingTasks();

        Assert.assertTrue(channel.attr(HeartbeatHandler.HEARTBEAT_FAIL_TIMES).get() >= 1);

        channel.finish();
    }

    @Test
    public void serverHeartbeatEvent() throws Exception {

        IdleStateHandler idleStateHandler = new IdleStateHandler(0L, 0L, 1L, TimeUnit.SECONDS);

        EmbeddedChannel channel = new EmbeddedChannel(idleStateHandler, new HeartbeatHandler());
        LongRpcChannel rpcChannel = Mockito.mock(LongRpcChannel.class);
        doNothing().when(rpcChannel).reconnect();
        doReturn(channel).when(rpcChannel).channel();
        doReturn(ChannelSide.SERVER).when(rpcChannel).side();
        channel.attr(RpcChannel.ATTRIBUTE_KEY).set(new ChannelAttribute(rpcChannel));

        TimeUnit.SECONDS.sleep(2);
        channel.runPendingTasks();

        Assert.assertFalse(channel.isActive());

        channel.finish();

    }

    @Test
    public void clientHeartbeatRead() {
        URI.Builder builder = new URI.Builder("atest", "localhost", 8006);
        LongRpcChannel rpcChannel = Mockito.mock(LongRpcChannel.class);
        doNothing().when(rpcChannel).reconnect();
        doNothing().when(rpcChannel).send(ArgumentMatchers.any());

        EmbeddedChannel channel = new EmbeddedChannel(new HeartbeatHandler());
        channel.attr(RpcChannel.ATTRIBUTE_KEY).set(new ChannelAttribute(rpcChannel));
        ATestProtocol testProtocol = new ATestProtocol();
        Response response = testProtocol.getHeartbeatTrigger().heartbeatResponse();

        channel.writeInbound(response);

        Assert.assertTrue(channel.attr(HeartbeatHandler.HEARTBEAT_FAIL_TIMES).get() == 0);
        Response response2 = channel.readInbound();
        Assert.assertTrue(response2 == null);

        channel.finish();
    }

    @Test
    public void serverHeartbeatRead() {
        URI.Builder builder = new URI.Builder("atest", "localhost", 8006);
        LongRpcChannel rpcChannel = Mockito.mock(LongRpcChannel.class);
        doNothing().when(rpcChannel).reconnect();
        doNothing().when(rpcChannel).send(ArgumentMatchers.any());

        EmbeddedChannel channel = new EmbeddedChannel(new HeartbeatHandler());
        channel.attr(RpcChannel.ATTRIBUTE_KEY).set(new ChannelAttribute(rpcChannel));
        ATestProtocol testProtocol = new ATestProtocol();
        Request request = testProtocol.getHeartbeatTrigger().heartbeatRequest();

        channel.writeInbound(request);

        Assert.assertTrue(channel.attr(HeartbeatHandler.HEARTBEAT_FAIL_TIMES).get() == 0);
        Request request2 = channel.readInbound();
        Assert.assertTrue(request2 == null);

        channel.finish();
    }

    @Test
    public void normalMsgRead() {
        URI.Builder builder = new URI.Builder("brpc", "localhost", 8006);
        LongRpcChannel rpcChannel = Mockito.mock(LongRpcChannel.class);
        doNothing().when(rpcChannel).reconnect();
        doNothing().when(rpcChannel).send(ArgumentMatchers.any());

        EmbeddedChannel channel = new EmbeddedChannel(new HeartbeatHandler());
        channel.attr(RpcChannel.ATTRIBUTE_KEY).set(new ChannelAttribute(rpcChannel));

        channel.writeInbound(new RpcResponse());
        Assert.assertTrue(channel.attr(HeartbeatHandler.HEARTBEAT_FAIL_TIMES).get() == 0);

        channel.writeInbound("Test");
        Assert.assertTrue(channel.attr(HeartbeatHandler.HEARTBEAT_FAIL_TIMES).get() == 0);

        channel.finish();
    }

}