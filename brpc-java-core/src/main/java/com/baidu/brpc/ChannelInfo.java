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

package com.baidu.brpc;

import com.baidu.brpc.buffer.DynamicCompositeByteBuf;
import com.baidu.brpc.client.FastFutureStore;
import com.baidu.brpc.client.RpcFuture;
import com.baidu.brpc.client.channel.BrpcChannel;
import com.baidu.brpc.client.channel.ChannelType;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.protocol.Protocol;
import com.baidu.brpc.protocol.Response;

import io.netty.channel.Channel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Setter
@Getter
@Slf4j
public class ChannelInfo {
    private static final AttributeKey<ChannelInfo> CLIENT_CHANNEL_KEY = AttributeKey.valueOf("client_key");
    private static final AttributeKey<ChannelInfo> SERVER_CHANNEL_KEY = AttributeKey.valueOf("server_key");

    private Channel channel;
    private BrpcChannel channelGroup;
    private Protocol protocol;
    private long logId;
    private FastFutureStore pendingRpc;
    private DynamicCompositeByteBuf recvBuf = new DynamicCompositeByteBuf(16);

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public static ChannelInfo getOrCreateClientChannelInfo(Channel channel) {
        Attribute<ChannelInfo> attribute = channel.attr(ChannelInfo.CLIENT_CHANNEL_KEY);
        ChannelInfo channelInfo = attribute.get();
        if (channelInfo == null) {
            channelInfo = new ChannelInfo();
            // 此时FastFutureStore单例对象已经在RpcClient创建时初始化过了
            channelInfo.setPendingRpc(FastFutureStore.getInstance(0));
            channelInfo.setChannel(channel);
            attribute.set(channelInfo);
        }
        return channelInfo;
    }

    public static ChannelInfo getClientChannelInfo(Channel channel) {
        Attribute<ChannelInfo> attribute = channel.attr(ChannelInfo.CLIENT_CHANNEL_KEY);
        return attribute.get();
    }

    public static ChannelInfo getOrCreateServerChannelInfo(Channel channel) {
        Attribute<ChannelInfo> attribute = channel.attr(ChannelInfo.SERVER_CHANNEL_KEY);
        ChannelInfo channelInfo = attribute.get();
        if (channelInfo == null) {
            channelInfo = new ChannelInfo();
            channelInfo.setChannel(channel);
            attribute.set(channelInfo);
        }
        return channelInfo;
    }

    public static ChannelInfo getServerChannelInfo(Channel channel) {
        Attribute<ChannelInfo> attribute = channel.attr(ChannelInfo.SERVER_CHANNEL_KEY);
        return  attribute.get();
    }

    public long addRpcFuture(RpcFuture future) {
        // FastFutureStore会保证返回的logId不会占用已经使用过的slot
        return pendingRpc.put(future);
    }

    public RpcFuture getRpcFuture(long logId) {
        return pendingRpc.get(logId);
    }

    public RpcFuture removeRpcFuture(long logId) {
        return pendingRpc.getAndRemove(logId);
    }

    /**
     * return channel when fail
     *
     * @param channelType
     */
    public void handleRequestFail(ChannelType channelType) {
        if (channelType != ChannelType.SHORT_CONNECTION) {
            channelGroup.incFailedNum();
            returnChannelAfterRequest();
        } else {
            channelGroup.close();
        }

    }

    /**
     * return channel when success
     */
    public void handleRequestSuccess() {
        returnChannelAfterRequest();
    }

    private void returnChannelAfterRequest() {
        if (protocol.returnChannelBeforeResponse()) {
            channelGroup.returnChannel(channel);
        }
    }

    /**
     * return channel when fail
     */
    public void handleResponseFail() {
        channelGroup.incFailedNum();
        returnChannelAfterResponse();
    }

    /**
     * return channel when success
     */
    public void handleResponseSuccess() {
        returnChannelAfterResponse();
    }

    private void returnChannelAfterResponse() {
        if (!protocol.returnChannelBeforeResponse()) {
            channelGroup.returnChannel(channel);
        }
    }

    /**
     * channel不可用时或者handler出现异常时处理逻辑
     */
    public void handleChannelException(RpcException ex) {
        if (channelGroup != null) {
            channelGroup.removeChannel(channel);
        }
        // 遍历并删除当前channel下所有RpcFuture
        pendingRpc.traverse(new ChannelErrorStoreWalker(channel, ex));
    }

    protected ChannelInfo() {
    }

    /**
     * 用于遍历FutureStore元素的实现类
     */
    private static class ChannelErrorStoreWalker implements FastFutureStore.StoreWalker {
        private Channel currentChannel;
        private RpcException exception;

        public ChannelErrorStoreWalker(Channel currentChannel, RpcException exception) {
            this.currentChannel = currentChannel;
            this.exception = exception;
        }

        @Override
        public boolean visitElement(RpcFuture fut) {
            // 与当前channel相同则删除
            ChannelInfo chanInfo = fut.getChannelInfo();
            if (null == chanInfo) {
                return true;
            }

            // 不删除返回true
            return currentChannel != chanInfo.channel;
        }

        @Override
        public void actionAfterDelete(RpcFuture fut) {
            Response response = fut.getRpcClient().getProtocol().createResponse();
            response.setException(exception);
            fut.handleResponse(response);
        }
    }
}
