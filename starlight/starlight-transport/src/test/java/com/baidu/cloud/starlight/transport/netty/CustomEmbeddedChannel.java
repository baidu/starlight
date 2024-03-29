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

import com.baidu.cloud.thirdparty.netty.channel.ChannelHandler;
import com.baidu.cloud.thirdparty.netty.channel.embedded.EmbeddedChannel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * Created by liuruisen on 2021/1/8.
 */
public class CustomEmbeddedChannel extends EmbeddedChannel {

    private InetSocketAddress socketAddress;

    public CustomEmbeddedChannel(String host, int port, final ChannelHandler... handlers) {
        super(handlers);
        socketAddress = new InetSocketAddress(host, port);
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return this.socketAddress;
    }
}
