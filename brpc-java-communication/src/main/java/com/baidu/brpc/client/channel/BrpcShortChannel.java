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

import com.baidu.brpc.ChannelInfo;
import com.baidu.brpc.client.CommunicationOptions;
import io.netty.channel.Channel;

import java.util.NoSuchElementException;

/**
 * BrpcShortChannel build single & short connection with server
 * and channel will be closed by brpc after communication with server
 */
public class BrpcShortChannel extends AbstractBrpcChannel {

    public BrpcShortChannel(ServiceInstance instance,
                            CommunicationOptions communicationOptions) {
        super(instance, communicationOptions);
    }

    @Override
    public Channel getChannel() throws Exception, NoSuchElementException, IllegalStateException {
        Channel channel = connect();
        ChannelInfo channelInfo = ChannelInfo.getOrCreateClientChannelInfo(channel);
        channelInfo.setProtocol(getProtocol());
        channelInfo.setChannelGroup(this);
        return channel;
    }

    @Override
    public void returnChannel(Channel channel) {
    }

    @Override
    public void removeChannel(Channel channel) {
        channel.close();
    }

    @Override
    public void close() {
    }

    @Override
    public void updateMaxConnection(int num) {
    }

    @Override
    public int getCurrentMaxConnection() {
        return getActiveConnectionNum();
    }

    @Override
    public int getActiveConnectionNum() {
        return 0;
    }

    @Override
    public int getIdleConnectionNum() {
        return 0;
    }

}
