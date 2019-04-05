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

package com.baidu.brpc.protocol;

import com.baidu.brpc.ChannelInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.util.concurrent.FastThreadLocal;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

public class RpcContext {
    private static final FastThreadLocal<RpcContext> CURRENT_RPC_CONTEXT = new FastThreadLocal<RpcContext>() {
        @Override
        protected RpcContext initialValue() {
            return new RpcContext();
        }
    };

    public static RpcContext getContext() {
        return CURRENT_RPC_CONTEXT.get();
    }

    public static void removeContext(RpcContext rpcContext) {
        if (rpcContext != null && rpcContext.getChannel() != null) {
            ChannelInfo channelInfo = ChannelInfo.getClientChannelInfo(rpcContext.getChannel());
            channelInfo.setFromRpcContext(false);
        }
        CURRENT_RPC_CONTEXT.remove();
    }

    public static void removeServerContext() {
        CURRENT_RPC_CONTEXT.remove();
    }

    public static void removeContext() {
        RpcContext rpcContext = CURRENT_RPC_CONTEXT.get();
        if (rpcContext != null && rpcContext.getChannel() != null) {
            ChannelInfo channelInfo = ChannelInfo.getClientChannelInfo(rpcContext.getChannel());
            channelInfo.setFromRpcContext(false);
        }
        CURRENT_RPC_CONTEXT.remove();
    }

    private Map<String, String> requestKvAttachment = new HashMap<String, String>();
    private ByteBuf requestBinaryAttachment;

    private Map<String, String> responseKvAttachment = new HashMap<String, String>();
    private ByteBuf responseBinaryAttachment;

    private Channel channel;

    private SocketAddress remoteAddress;

    public void reset() {
        requestKvAttachment.clear();
        requestBinaryAttachment = null;
        responseKvAttachment.clear();
        responseBinaryAttachment = null;
        channel = null;
        remoteAddress = null;
    }

    public void putRequestKvAttachment(String key, String value) {
        requestKvAttachment.put(key, value);
    }

    public void setRequestKvAttachment(Map<String, String> requestKvAttachment) {
        this.requestKvAttachment = requestKvAttachment;
    }

    public void setRequestBinaryAttachment(ByteBuf byteBuf) {
        this.requestBinaryAttachment = Unpooled.wrappedBuffer(byteBuf);
    }

    public void setRequestBinaryAttachment(byte[] bytes) {
        this.requestBinaryAttachment = Unpooled.wrappedBuffer(bytes);
    }

    public void setResponseKvAttachment(Map<String, String> responseKvAttachment) {
        this.responseKvAttachment = responseKvAttachment;
    }

    public void setResponseBinaryAttachment(ByteBuf responseBinaryAttachment) {
        this.responseBinaryAttachment = responseBinaryAttachment;
    }

    public Map<String, String> getRequestKvAttachment() {
        return requestKvAttachment;
    }

    public ByteBuf getRequestBinaryAttachment() {
        return requestBinaryAttachment;
    }

    public Map<String, String> getResponseKvAttachment() {
        return responseKvAttachment;
    }

    public ByteBuf getResponseBinaryAttachment() {
        return responseBinaryAttachment;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
        if (channel != null) {
            ChannelInfo channelInfo = ChannelInfo.getClientChannelInfo(channel);
            if (channelInfo != null) {
                channelInfo.setFromRpcContext(true);
            }
        }
    }

    public void setRemoteAddress(SocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public String getRemoteHost() {
        if (remoteAddress != null) {
            InetSocketAddress remoteAddress = (InetSocketAddress) this.remoteAddress;
            InetAddress address = remoteAddress.getAddress();
            return address.getHostAddress();
        } else {
            return null;
        }
    }
}
