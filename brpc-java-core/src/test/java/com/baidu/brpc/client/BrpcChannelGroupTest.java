/*
 * Copyright (c) 2018 Baidu, Inc. All Rights Reserved.
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
package com.baidu.brpc.client;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Queue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.baidu.brpc.client.channel.BrpcChannel;
import com.baidu.brpc.client.channel.BrpcPooledChannel;
import com.baidu.brpc.server.RpcServer;

import io.netty.channel.Channel;

public class BrpcChannelGroupTest {

    private RpcServer rpcServer;

    private RpcClient rpcClient;

    private BrpcChannel channelGroup;

    private RpcClientOptions options;

    @Before
    public void before() {
        rpcServer = new RpcServer(8000);
        rpcServer.start();
        options = new RpcClientOptions();
        options.setLatencyWindowSizeOfFairLoadBalance(2);
        rpcClient = new RpcClient("list://127.0.0.1:8000", options);
        channelGroup = new BrpcPooledChannel("127.0.0.1", 8000, rpcClient);
    }

    @After
    public void after() {
        if (rpcClient != null) {
            rpcClient.stop();
        }
        if (rpcServer != null) {
            rpcServer.shutdown();
        }
    }

    @Test
    public void test() throws Exception {
        Channel channel = channelGroup.getChannel();
        assertThat(channel.isActive(), is(true));
        channelGroup.returnChannel(channel);
        channel = channelGroup.connect("127.0.0.1", 8000);
        assertThat(channel.isActive(), is(true));
        channel.close();
        channelGroup.updateLatency(10);
        channelGroup.updateLatencyWithReadTimeOut();
        Queue<Integer> latencyWindow = channelGroup.getLatencyWindow();
        assertThat(latencyWindow.poll(), is(10));
        assertThat(latencyWindow.poll(), is(options.getReadTimeoutMillis()));
    }
}
