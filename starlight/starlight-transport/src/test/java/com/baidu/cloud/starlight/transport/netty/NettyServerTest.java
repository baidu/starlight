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
import com.baidu.cloud.starlight.api.model.MsgBase;
import com.baidu.cloud.starlight.api.rpc.Processor;
import com.baidu.cloud.starlight.api.rpc.ServiceRegistry;
import com.baidu.cloud.starlight.api.rpc.threadpool.ThreadPoolFactory;
import com.baidu.cloud.starlight.api.transport.channel.RpcChannel;
import com.baidu.cloud.thirdparty.netty.bootstrap.ServerBootstrap;
import com.baidu.cloud.thirdparty.netty.channel.ServerChannel;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

/**
 * Created by liuruisen on 2020/3/23.
 */
public class NettyServerTest {

    @Test
    public void init() throws NoSuchFieldException, IllegalAccessException {
        URI.Builder builder = new URI.Builder("brpc", "localhost", 8006);
        NettyServer nettyServer = new NettyServer(builder.build());
        nettyServer.init();
        Field field = nettyServer.getClass().getDeclaredField("bootstrap");
        field.setAccessible(true);
        ServerBootstrap bootstrap = (ServerBootstrap) field.get(nettyServer);
        Assert.assertTrue(bootstrap != null);
    }

    @Test
    public void bindAndClose() throws NoSuchFieldException, IllegalAccessException, InterruptedException {
        URI.Builder builder = new URI.Builder("brpc", "localhost", 8006);
        NettyServer nettyServer = new NettyServer(builder.build());
        nettyServer.init();
        nettyServer.bind();
        TimeUnit.SECONDS.sleep(1);

        Field field = nettyServer.getClass().getDeclaredField("serverChannel");
        field.setAccessible(true);
        ServerChannel channel = (ServerChannel) field.get(nettyServer);
        Assert.assertTrue(channel.isActive());
        Assert.assertTrue(nettyServer.isBound());

        nettyServer.close();
        TimeUnit.SECONDS.sleep(1);
        Assert.assertFalse(nettyServer.isBound());
    }

    @Test
    public void getUri() {
        URI.Builder builder = new URI.Builder("brpc", "localhost", 8006);
        NettyServer nettyServer = new NettyServer(builder.build());
        Assert.assertTrue(nettyServer.getUri().getPort() == 8006);
    }

    @Test
    public void setProcessor() {
        URI.Builder builder = new URI.Builder("brpc", "localhost", 8006);
        NettyServer nettyServer = new NettyServer(builder.build());
        nettyServer.setProcessor(new Processor() {
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
                return 0;
            }

            @Override
            public Integer processingCount(String serviceKey) {
                return 0;
            }

            @Override
            public Long completeCount(String serviceKey) {
                return 0l;
            }

            @Override
            public Integer allWaitTaskCount() {
                return 0;
            }
        });
        Assert.assertTrue(nettyServer.getProcessor() != null);
    }
}