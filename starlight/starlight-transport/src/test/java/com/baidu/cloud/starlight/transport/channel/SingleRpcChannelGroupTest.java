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
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import com.baidu.cloud.starlight.api.transport.channel.RpcChannel;
import com.baidu.cloud.thirdparty.netty.bootstrap.Bootstrap;
import com.baidu.cloud.thirdparty.netty.channel.Channel;
import com.baidu.cloud.thirdparty.netty.channel.ChannelFuture;
import com.baidu.cloud.thirdparty.netty.channel.ChannelId;
import com.baidu.cloud.thirdparty.netty.channel.DefaultChannelId;
import com.baidu.cloud.thirdparty.netty.util.Attribute;
import com.baidu.cloud.thirdparty.netty.util.Timeout;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

/**
 * Created by liuruisen on 2020/12/7.
 */
public class SingleRpcChannelGroupTest {

    private URI uri;

    private Bootstrap bootstrap;

    @Before
    public void before() {
        String baseUriString = "brpc://admin:hello1234@10.20.30.40:20880/context/path?app=mail";
        uri = URI.valueOf(baseUriString);

        bootstrap = Mockito.mock(Bootstrap.class);

        ChannelFuture channelFuture = Mockito.mock(ChannelFuture.class);
        doReturn(true).when(channelFuture).awaitUninterruptibly(ArgumentMatchers.anyLong());
        doReturn(true).when(channelFuture).isSuccess();

        doReturn(channelFuture).when(bootstrap).connect(any());

        Channel channel = Mockito.mock(Channel.class);
        doReturn(channel).when(channelFuture).channel();

        ChannelId channelId = DefaultChannelId.newInstance();
        Attribute attribute = Mockito.mock(Attribute.class);
        doReturn(attribute).when(channel).attr(any());
        doReturn(channelId).when(channel).id();
        doNothing().when(attribute).set(any());
        doReturn(channelFuture).when(channel).close();
        doReturn(true).when(channel).isActive();
        doReturn(channelFuture).when(channel).writeAndFlush(any());
    }

    @Test
    public void getRpcChannel() {
        SingleRpcChannelGroup singleRpcChannelGroup = new SingleRpcChannelGroup(uri, bootstrap);
        singleRpcChannelGroup.init();
        RpcChannel rpcChannel = singleRpcChannelGroup.getRpcChannel();
        assertNotNull(rpcChannel);

        // mock request 1
        new Thread(() -> {
            rpcChannel.putCallback(1L, new RpcCallback() {
                @Override
                public void addTimeout(Timeout timeout) {}

                @Override
                public Request getRequest() {
                    return null;
                }

                @Override
                public void onResponse(Response response) {}

                @Override
                public void onError(Throwable e) {}
            });

            try {
                TimeUnit.SECONDS.sleep(30); // Simulate request processing
            } catch (InterruptedException e) {
                // ignore
            }
            rpcChannel.removeCallback(1L);
        }).start();

        try {
            TimeUnit.MILLISECONDS.sleep(200);
        } catch (InterruptedException e) {
            // ignore
        }
        assertEquals(1, rpcChannel.allCallbacks().size());

        // mock disconnect cause by heartbeat fail
        singleRpcChannelGroup.removeRpcChannel(rpcChannel);
        // wait for reconnect task exec
        try {
            TimeUnit.MILLISECONDS.sleep(200);
        } catch (InterruptedException e) {
            // ignore
        }

        // mock request 2
        RpcChannel rpcChannel2 = singleRpcChannelGroup.getRpcChannel();
        assertNotNull(rpcChannel2);
        assertTrue(rpcChannel != rpcChannel2);

        // another request
        new Thread(() -> {
            rpcChannel2.putCallback(2L, new RpcCallback() {
                @Override
                public void addTimeout(Timeout timeout) {}

                @Override
                public Request getRequest() {
                    return null;
                }

                @Override
                public void onResponse(Response response) {}

                @Override
                public void onError(Throwable e) {}
            });

            try {
                TimeUnit.SECONDS.sleep(3); // Simulate request processing
            } catch (InterruptedException e) {
                // ignore
            }
            rpcChannel.removeCallback(1L);
        }).start();

        try {
            TimeUnit.MILLISECONDS.sleep(200);
        } catch (InterruptedException e) {
            // ignore
        }

        // wait for old channel close
        try {
            TimeUnit.SECONDS.sleep(40);
        } catch (InterruptedException e) {
            // ignore
        }

        assertEquals(0, rpcChannel.allCallbacks().size());
    }

    @Test
    public void rpcChannelCount() {
        SingleRpcChannelGroup singleRpcChannelGroup = new SingleRpcChannelGroup(uri, bootstrap);
        assertEquals(1, singleRpcChannelGroup.rpcChannelCount());
    }

    @Test
    public void allRpcChannels() {
        SingleRpcChannelGroup singleRpcChannelGroup = new SingleRpcChannelGroup(uri, bootstrap);
        assertEquals(1, singleRpcChannelGroup.allRpcChannels().size());
    }
}