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
 
package com.baidu.cloud.starlight.api.transport.channel;

import com.baidu.cloud.thirdparty.netty.channel.Channel;

/**
 * ThreadLocal Netty Channel Context Store thread-local netty {@link Channel} information, including netty channel and
 * {@link ChannelAttribute}
 * <p>
 * Created by liuruisen on 2020/11/9.
 */
public class ThreadLocalChannelContext {

    private static final ThreadLocal<ThreadLocalChannelContext> CHANNEL_CONTEXT =
        ThreadLocal.withInitial(ThreadLocalChannelContext::new);

    public static ThreadLocalChannelContext getContext() {
        return CHANNEL_CONTEXT.get();
    }

    public static void removeContext() {
        CHANNEL_CONTEXT.remove();
    }

    private Channel channel;

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public ChannelAttribute getChannelAttribute() {
        if (channel == null) {
            return null;
        }
        return channel.attr(RpcChannel.ATTRIBUTE_KEY).get();
    }
}
