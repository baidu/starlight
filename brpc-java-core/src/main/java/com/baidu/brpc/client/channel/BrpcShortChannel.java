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
package com.baidu.brpc.client.channel;


import java.util.NoSuchElementException;

import com.baidu.brpc.ChannelInfo;
import com.baidu.brpc.client.RpcClient;

import io.netty.channel.Channel;

/**
 * BrpcShortChannel build single & short connection with server
 * and channel will be closed by brpc after communication with server
 */
public class BrpcShortChannel extends AbstractBrpcChannel {

    private volatile Channel channel;

    public BrpcShortChannel(String ip, int port, RpcClient rpcClient) {
        super(ip, port, rpcClient.getBootstrap(), rpcClient.getProtocol());
    }

    @Override
    public Channel getChannel() throws Exception, NoSuchElementException, IllegalStateException {

        if (channel == null || !channel.isActive()) {
            synchronized (this) {
                if (channel != null && !channel.isActive()) {
                    channel.close();
                    channel = null;
                }
                if (channel == null) {
                    channel = connect(ip, port);
                    ChannelInfo channelInfo = ChannelInfo.getOrCreateClientChannelInfo(channel);
                    channelInfo.setProtocol(protocol);
                    channelInfo.setChannelGroup(this);

                }
            }
        }
        return channel;
    }

    @Override
    public void returnChannel(Channel channel) {

    }

    @Override
    public void removeChannel(Channel channel) {
        closeChannel();
    }

    @Override
    public void close() {
        closeChannel();
    }

    @Override
    public void updateMaxConnection(int num) {

        // do nothing

    }

    @Override
    public int getCurrentMaxConnection() {
        return getActiveConnectionNum();
    }

    @Override
    public int getActiveConnectionNum() {
        if (channel != null && channel.isActive()) {
            return 1;
        }

        return 0;
    }

    @Override
    public int getIdleConnectionNum() {
        if (channel == null || !channel.isActive()) {
            return 1;
        }
        return 0;
    }

    private void closeChannel() {
        if (channel != null && channel.isActive()) {
            channel.close();
        }
    }
}
