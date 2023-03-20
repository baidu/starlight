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

import com.baidu.cloud.starlight.api.common.URI;
import com.baidu.cloud.starlight.api.exception.StarlightRpcException;
import com.baidu.cloud.starlight.api.model.MsgBase;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.rpc.Processor;
import com.baidu.cloud.starlight.api.rpc.ServiceRegistry;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import com.baidu.cloud.starlight.api.rpc.threadpool.ThreadPoolFactory;
import com.baidu.cloud.starlight.api.transport.PeerStatus;
import com.baidu.cloud.starlight.api.transport.channel.RpcChannel;
import com.baidu.cloud.starlight.api.transport.channel.RpcChannelGroup;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelId;
import io.netty.channel.DefaultChannelId;
import io.netty.util.Attribute;
import io.netty.util.Timeout;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

/**
 * Created by liuruisen on 2020/3/23.
 */
public class NettyClientTest {

    @Test
    public void init() throws NoSuchFieldException, IllegalAccessException {
        URI.Builder builder = new URI.Builder("brpc", "localhost", 8006);
        NettyClient nettyClient = new NettyClient(builder.build());
        nettyClient.init();
        Field field = nettyClient.getClass().getDeclaredField("bootstrap");
        field.setAccessible(true);
        Bootstrap bootstrap = (Bootstrap) field.get(nettyClient);
        Assert.assertTrue(bootstrap != null);
    }

    @Test
    public void connect() throws NoSuchFieldException, IllegalAccessException {
        Bootstrap bootstrap = Mockito.mock(Bootstrap.class);
        ChannelFuture channelFuture = Mockito.mock(ChannelFuture.class);
        doReturn(channelFuture).when(bootstrap).connect(ArgumentMatchers.any());
        doReturn(true).when(channelFuture).awaitUninterruptibly(ArgumentMatchers.anyLong());
        doReturn(true).when(channelFuture).isSuccess();
        Channel channel = Mockito.mock(Channel.class);
        doReturn(channel).when(channelFuture).channel();
        Attribute attribute = Mockito.mock(Attribute.class);
        doReturn(attribute).when(channel).attr(ArgumentMatchers.any());
        ChannelId channelId = DefaultChannelId.newInstance();
        doReturn(channelId).when(channel).id();
        doNothing().when(attribute).set(ArgumentMatchers.any());

        URI.Builder builder = new URI.Builder("brpc", "localhost", 8006);
        NettyClient nettyClient = new NettyClient(builder.build());
        nettyClient.init();
        Field field = nettyClient.getClass().getDeclaredField("bootstrap");
        field.setAccessible(true);
        field.set(nettyClient, bootstrap);

        nettyClient.connect();

        RpcChannelGroup rpcChannelGroup = nettyClient.getChannelGroup();
        Assert.assertTrue(rpcChannelGroup != null);
        Assert.assertTrue(rpcChannelGroup.getUri().getProtocol().equals("brpc"));
    }

    @Test
    public void request() throws NoSuchFieldException, IllegalAccessException {
        URI.Builder builder = new URI.Builder("brpc", "localhost", 8006);
        NettyClient nettyClient = new NettyClient(builder.build());

        RpcChannel rpcChannel = Mockito.mock(RpcChannel.class);
        doNothing().when(rpcChannel).putCallback(ArgumentMatchers.anyLong(), ArgumentMatchers.any());
        doNothing().when(rpcChannel).send(ArgumentMatchers.any());

        RpcChannelGroup channelGroup = Mockito.mock(RpcChannelGroup.class);
        doReturn(rpcChannel).when(channelGroup).getRpcChannel();
        Field field = nettyClient.getClass().getDeclaredField("rpcChannelGroup");
        field.setAccessible(true);
        field.set(nettyClient, channelGroup);
        nettyClient.request(new RpcRequest(), new RpcCallback() {
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

    }

    @Test
    public void getUri() {
        URI.Builder builder = new URI.Builder("brpc", "localhost", 8006);
        NettyClient nettyClient = new NettyClient(builder.build());
        URI uri = nettyClient.getUri();
        Assert.assertTrue(uri.getPort() == 8006);
    }

    @Test
    public void closeNull() {
        URI.Builder builder = new URI.Builder("brpc", "localhost", 8006);
        NettyClient nettyClient = new NettyClient(builder.build());
        nettyClient.close();
    }

    @Test
    public void close() {
        URI.Builder builder = new URI.Builder("brpc", "localhost", 8006);
        NettyClient nettyClient = new NettyClient(builder.build());
        nettyClient.setProcessor(new Processor() {
            @Override
            public ServiceRegistry getRegistry() {
                return null;
            }

            @Override
            public void process(MsgBase msgBase, RpcChannel context) {

            }

            @Override
            public void close() {

            }

            @Override
            public void setThreadPoolFactory(ThreadPoolFactory threadPoolFactory) {

            }

            @Override
            public Integer waitTaskCount(String serviceKey) {
                return null;
            }

            @Override
            public Integer processingCount(String serviceKey) {
                return null;
            }

            @Override
            public Long completeCount(String serviceKey) {
                return null;
            }

            @Override
            public Integer allWaitTaskCount() {
                return null;
            }
        });
        nettyClient.close();
    }

    @Test
    public void setProcessor() {
        Processor clientProcessor = new Processor() {
            @Override
            public ServiceRegistry getRegistry() {
                return null;
            }

            @Override
            public void process(MsgBase msgBase, RpcChannel context) {

            }

            @Override
            public void close() {

            }

            @Override
            public void setThreadPoolFactory(ThreadPoolFactory threadPoolFactory) {

            }

            @Override
            public Integer waitTaskCount(String serviceKey) {
                return null;
            }

            @Override
            public Integer processingCount(String serviceKey) {
                return null;
            }

            @Override
            public Long completeCount(String serviceKey) {
                return null;
            }

            @Override
            public Integer allWaitTaskCount() {
                return null;
            }
        };
        URI.Builder builder = new URI.Builder("brpc", "localhost", 8006);
        NettyClient nettyClient = new NettyClient(builder.build());
        nettyClient.setProcessor(clientProcessor);

        Assert.assertTrue(nettyClient.getProcessor() == clientProcessor);
    }

    @Test
    public void rpcChannelGroup() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        URI.Builder builder = new URI.Builder("brpc", "localhost", 8006);
        NettyClient nettyClient = new NettyClient(builder.build());

        Method method = nettyClient.getClass().getDeclaredMethod("rpcChannelGroup", String.class);
        method.setAccessible(true);

        try {
            method.invoke(nettyClient, new Object[] {null});
        } catch (Exception e) {
            Assert.assertTrue(e.getCause().getMessage().equals("RpcChannelGroup type is null"));
        }

        try {
            method.invoke(nettyClient, "test");
        } catch (Exception e) {
            Assert.assertTrue(e.getCause() instanceof StarlightRpcException);
            Assert.assertTrue(e.getCause().getMessage().contains("is illegal: not support"));
        }
    }

    @Test
    public void updateStatus() {
        URI.Builder builder = new URI.Builder("brpc", "localhost", 8006);
        NettyClient nettyClient = new NettyClient(builder.build());
        nettyClient.updateStatus(new PeerStatus(PeerStatus.Status.ACTIVE, System.currentTimeMillis()));

        assertEquals(PeerStatus.Status.ACTIVE, nettyClient.status().getStatus());
    }

    @Test
    public void updateStatusShuttingDown() {
        URI.Builder builder = new URI.Builder("brpc", "localhost", 8006);
        NettyClient nettyClient = new NettyClient(builder.build());
        nettyClient.updateStatus(new PeerStatus(PeerStatus.Status.SHUTTING_DOWN, System.currentTimeMillis()));
        assertEquals(PeerStatus.Status.SHUTTING_DOWN, nettyClient.status().getStatus());

        nettyClient.updateStatus(new PeerStatus(PeerStatus.Status.OUTLIER, System.currentTimeMillis()));
        assertEquals(PeerStatus.Status.SHUTTING_DOWN, nettyClient.status().getStatus());

        nettyClient.updateStatus(new PeerStatus(PeerStatus.Status.SHUTDOWN, System.currentTimeMillis()));
        assertEquals(PeerStatus.Status.SHUTDOWN, nettyClient.status().getStatus());

        nettyClient.updateStatus(new PeerStatus(PeerStatus.Status.OUTLIER, System.currentTimeMillis()));
        assertEquals(PeerStatus.Status.SHUTDOWN, nettyClient.status().getStatus());
    }

    @Test
    public void updateStatusOutlier() {
        URI.Builder builder = new URI.Builder("brpc", "localhost", 8006);
        NettyClient nettyClient = new NettyClient(builder.build());

        long outlierTime1 = System.currentTimeMillis();
        PeerStatus peerStatus = new PeerStatus(PeerStatus.Status.OUTLIER, outlierTime1);
        nettyClient.updateStatus(peerStatus);
        assertEquals(peerStatus, nettyClient.status());

        PeerStatus peerStatusBefore = new PeerStatus(PeerStatus.Status.OUTLIER, outlierTime1 - 1);
        nettyClient.updateStatus(peerStatusBefore);
        assertEquals(peerStatus, nettyClient.status());

        PeerStatus peerStatusAfter = new PeerStatus(PeerStatus.Status.OUTLIER, outlierTime1 + 1);
        nettyClient.updateStatus(peerStatusAfter);
        assertNotEquals(peerStatusAfter, nettyClient.status());

    }

}