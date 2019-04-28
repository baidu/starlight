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

package com.baidu.brpc.client.pool;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import com.baidu.brpc.ChannelInfo;
import com.baidu.brpc.client.channel.BrpcChannel;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

/**
 * Channel pool for the rpc client.
 * Base on apache.commons.pool.
 */
@Slf4j
public class ChannelPooledObjectFactory extends BasePooledObjectFactory<Channel> {

    private BrpcChannel channelGroup;

    private String ip;

    private int port;

    public ChannelPooledObjectFactory(BrpcChannel channelGroup, String ip, int port) {
        this.channelGroup = channelGroup;
        this.ip = ip;
        this.port = port;
    }

    @Override
    public Channel create() throws Exception {
        Channel channel = channelGroup.connect(ip, port);
        ChannelInfo channelInfo = ChannelInfo.getOrCreateClientChannelInfo(channel);
        channelInfo.setChannelGroup(channelGroup);
        channelInfo.setProtocol(channelGroup.getProtocol());
        return channel;
    }

    @Override
    public PooledObject<Channel> wrap(Channel obj) {
        return new DefaultPooledObject<Channel>(obj);
    }

    @Override
    public void destroyObject(PooledObject<Channel> p) throws Exception {
        Channel channel = p.getObject();
        if (channel != null && channel.isOpen() && channel.isActive()) {
            channel.close();
        }
        channel = null;
    }

    public boolean validateObject(PooledObject<Channel> p) {
        Channel channel = p.getObject();
        return channel != null && channel.isOpen() && channel.isActive();
    }

}