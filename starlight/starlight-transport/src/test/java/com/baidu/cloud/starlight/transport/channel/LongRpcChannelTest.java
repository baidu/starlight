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
 
package com.baidu.cloud.starlight.transport.channel;

import com.baidu.cloud.starlight.api.common.URI;
import com.baidu.cloud.starlight.api.exception.TransportException;
import com.baidu.cloud.starlight.api.model.AbstractMsgBase;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import com.baidu.cloud.starlight.api.transport.channel.ChannelSide;
import com.baidu.cloud.starlight.api.transport.channel.RpcChannel;
import com.baidu.cloud.thirdparty.netty.bootstrap.Bootstrap;
import com.baidu.cloud.thirdparty.netty.channel.Channel;
import com.baidu.cloud.thirdparty.netty.channel.ChannelFuture;
import com.baidu.cloud.thirdparty.netty.channel.ChannelId;
import com.baidu.cloud.thirdparty.netty.channel.DefaultChannelId;
import com.baidu.cloud.thirdparty.netty.util.Attribute;
import com.baidu.cloud.thirdparty.netty.util.Timeout;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

/**
 * Created by liuruisen on 2020/3/23.
 */
public class LongRpcChannelTest {

    private Bootstrap bootstrap;

    private URI uri;

    private LongRpcChannel rpcChannel;

    private Channel channel;

    @Before
    public void before() {
        URI.Builder builder = new URI.Builder("brpc", "localhost", 8006);
        uri = builder.build();

        bootstrap = Mockito.mock(Bootstrap.class);

        ChannelFuture channelFuture = Mockito.mock(ChannelFuture.class);
        doReturn(true).when(channelFuture).awaitUninterruptibly(ArgumentMatchers.anyLong());
        doReturn(true).when(channelFuture).isSuccess();

        doReturn(channelFuture).when(bootstrap).connect(any());

        channel = Mockito.mock(Channel.class);
        doReturn(channel).when(channelFuture).channel();

        ChannelId channelId = DefaultChannelId.newInstance();
        Attribute attribute = Mockito.mock(Attribute.class);
        doReturn(attribute).when(channel).attr(any());
        doReturn(channelId).when(channel).id();
        doNothing().when(attribute).set(any());
        doReturn(channelFuture).when(channel).close();
        doReturn(true).when(channel).isActive();
        doReturn(channelFuture).when(channel).writeAndFlush(any());

        rpcChannel = new LongRpcChannel(channel, ChannelSide.CLIENT);
    }

    @Test
    public void init() {
        RpcChannel rpcChannel = new LongRpcChannel(channel, ChannelSide.CLIENT);
        rpcChannel.init();
        Channel channel = rpcChannel.channel();
        Assert.assertTrue(channel.isActive());
        rpcChannel.init();
        Assert.assertEquals(channel, rpcChannel.channel());
    }

    @Test
    public void isActive() {
        RpcChannel rpcChannel = new LongRpcChannel(channel, ChannelSide.CLIENT);
        assertTrue(rpcChannel.isActive());
        rpcChannel.close();
        assertFalse(rpcChannel.isActive());
    }

    @Test
    public void reconnect() {
        RpcChannel rpcChannel = new LongRpcChannel(channel, ChannelSide.CLIENT);
        rpcChannel.reconnect();
        Channel channel = rpcChannel.channel();
        Assert.assertTrue(channel.isActive());
    }

    @Test
    public void channel() {
        RpcChannel rpcChannel = new LongRpcChannel(channel, ChannelSide.CLIENT);
        rpcChannel.channel();
        Assert.assertTrue(channel.isActive());
    }

    @Test
    public void close() throws IllegalAccessException, NoSuchFieldException {
        RpcChannel rpcChannel = new LongRpcChannel(channel, ChannelSide.CLIENT);

        new Thread(() -> {
            rpcChannel.putCallback(1L, new RpcCallback() {
                @Override
                public void addTimeout(Timeout timeout) {

                }

                @Override
                public Request getRequest() {
                    return null;
                }

                @Override
                public void onResponse(Response response) {

                }

                @Override
                public void onError(Throwable e) {

                }
            });

            try {
                TimeUnit.SECONDS.sleep(20); // Simulate request processing
            } catch (InterruptedException e) {
                // ignore
            }
            rpcChannel.removeCallback(1L);
        }).start();

        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
            // ignore
        }

        assertEquals(1, rpcChannel.allCallbacks().size());
        assertTrue(rpcChannel.isActive());

        rpcChannel.close();

        assertEquals(0, rpcChannel.allCallbacks().size());
        assertFalse(rpcChannel.isActive());

        Field field = rpcChannel.getClass().getDeclaredField("channel");
        field.setAccessible(true);

        Assert.assertNotNull(field.get(rpcChannel));
    }

    @Test
    public void send() {
        RpcChannel rpcChannel = new LongRpcChannel(channel, ChannelSide.CLIENT);
        rpcChannel.send(new RpcRequest());
    }

    @Test
    public void sendError() {
        Bootstrap bootstrap = Mockito.mock(Bootstrap.class);

        ChannelFuture connectFuture = Mockito.mock(ChannelFuture.class);
        doReturn(true).when(connectFuture).awaitUninterruptibly(ArgumentMatchers.anyLong());
        doReturn(true).when(connectFuture).isSuccess();

        doReturn(connectFuture).when(bootstrap).connect(any());

        Channel channel = Mockito.mock(Channel.class);
        ChannelId channelId = DefaultChannelId.newInstance();
        doReturn(channel).when(connectFuture).channel();
        doReturn(true).when(channel).isActive();
        Attribute attribute = Mockito.mock(Attribute.class);
        doReturn(attribute).when(channel).attr(any());
        doReturn(channelId).when(channel).id();
        doNothing().when(attribute).set(any());

        ChannelFuture writeFuture = Mockito.mock(ChannelFuture.class);
        doReturn(true).when(writeFuture).awaitUninterruptibly(ArgumentMatchers.anyLong());
        doReturn(false).when(writeFuture).isSuccess();

        doReturn(writeFuture).when(channel).writeAndFlush(any());

        RpcChannel longChannel = new LongRpcChannel(channel, ChannelSide.CLIENT);
        // channel is not active
        try {
            longChannel.send(new RpcRequest());
        } catch (TransportException e) {
            Assert.assertEquals(e.getMessage(), "Remote Channel is inactive");
        }

        doReturn(true).when(channel).isActive();
        Exception exception = new Exception("Test");
        doReturn(exception).when(writeFuture).cause();
        // send fail
        try {
            longChannel.send(new AbstractMsgBase() {});
        } catch (TransportException e) {
            Assert.assertEquals(e.getCode(), TransportException.WRITE_EXCEPTION);
        }
    }
}