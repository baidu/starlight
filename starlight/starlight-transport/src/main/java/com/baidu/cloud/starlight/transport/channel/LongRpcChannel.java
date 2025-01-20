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
 
package com.baidu.cloud.starlight.transport.channel;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.exception.StarlightRpcException;
import com.baidu.cloud.starlight.api.exception.TransportException;
import com.baidu.cloud.starlight.api.model.MsgBase;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.rpc.LocalContext;
import com.baidu.cloud.starlight.api.rpc.callback.Callback;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import com.baidu.cloud.starlight.api.transport.channel.ChannelAttribute;
import com.baidu.cloud.starlight.api.transport.channel.ChannelSide;
import com.baidu.cloud.starlight.api.transport.channel.RpcChannel;
import com.baidu.cloud.starlight.api.transport.channel.RpcChannelGroup;
import com.baidu.cloud.starlight.protocol.http.springrest.SpringRestProtocol;
import com.baidu.cloud.thirdparty.netty.channel.Channel;
import com.baidu.cloud.thirdparty.netty.channel.ChannelFuture;
import com.baidu.cloud.thirdparty.netty.channel.ChannelFutureListener;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpHeaderNames;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpHeaderValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Long Single Channel Created by liuruisen on 2020/2/3.
 */
public class LongRpcChannel implements RpcChannel {

    protected static final Logger LOGGER = LoggerFactory.getLogger(NettyRpcChannelGroup.class);

    private final Map<Long, RpcCallback> callbacks;

    private final Map<String, Object> attributes;

    private volatile Channel channel;

    private final ChannelSide side;

    private RpcChannelGroup channelGroup;

    private AtomicBoolean inited = new AtomicBoolean(false);

    public LongRpcChannel(Channel channel, ChannelSide side) {
        this(channel, side, null);
    }

    public LongRpcChannel(Channel channel, ChannelSide side, RpcChannelGroup channelGroup) {
        this.callbacks = new ConcurrentHashMap<>();
        this.attributes = new ConcurrentHashMap<>();
        this.channel = channel;
        this.side = side;
        this.channelGroup = channelGroup;
        init();
    }

    @Override
    public void init() {
        if (!inited.compareAndSet(false, true)) {
            return;
        }

        ChannelAttribute attribute = new ChannelAttribute(this);
        channel.attr(RpcChannel.ATTRIBUTE_KEY).set(attribute);
    }

    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    public ChannelSide side() {
        return side;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        if (channel == null) {
            return null;
        }
        return (InetSocketAddress) channel.remoteAddress();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        if (channel == null) {
            return null;
        }
        return (InetSocketAddress) channel.localAddress();
    }

    @Override
    public boolean isActive() {
        return channel != null && channel.isActive() && inited.get();
    }

    @Override
    public void reconnect() throws TransportException {
        if (getRpcChannelGroup() != null) {
            getRpcChannelGroup().removeRpcChannel(this); // remove and reconnect
        }
    }

    @Override
    public void send(MsgBase msgBase) throws TransportException {
        if (!isActive()) {
            throw new TransportException(TransportException.WRITE_EXCEPTION, "Channel is inactive, " + "remoteHost: "
                + getRemoteAddress().getAddress() + ", remotePort: " + getRemoteAddress().getPort());
        }
        // write and flush message
        ChannelFuture sendFuture = channel.writeAndFlush(msgBase);
        sendFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if (!channelFuture.isSuccess()) {
                    String causeByMsg =
                        channelFuture.cause() == null ? "netty exception" : channelFuture.cause().getMessage();
                    String errorMsg = String.format("Fail to send message to remote, url[%s:%s], error message: %s",
                        getRemoteAddress().getAddress(), getRemoteAddress().getPort(), causeByMsg);
                    if (side().equals(ChannelSide.CLIENT)) {
                        Callback callback = removeCallback(msgBase.getId());
                        if (callback != null) {
                            callback.onError(new TransportException(TransportException.WRITE_EXCEPTION, errorMsg));
                        }
                    }
                    LOGGER.warn(errorMsg);
                }

                // http protocol and short connection: Connection:close
                // fixme 关闭连接放在了send之后，是不是不太合理？
                if (side().equals(ChannelSide.SERVER)
                    && msgBase.getProtocolName().equalsIgnoreCase(SpringRestProtocol.PROTOCOL_NAME)
                    && isHttpShortConnection(msgBase)) {
                    channelFuture.channel().close();
                }
            }
        });
    }

    @Override
    public void receive(MsgBase msgBase) {
        // FIXME find reasonable use: do some biz operations such as metrics
        // LOGGER.debug("Receive Message from remote peer, RequestId: " + msgBase.getId());
    }

    @Override
    public void putCallback(long id, RpcCallback callback) {
        if (side().equals(ChannelSide.SERVER)) {
            throw new StarlightRpcException(
                "RpcChannel side is SERVER, not support putCallback. " + "RequestId: " + id);
        }
        callbacks.putIfAbsent(id, callback);
    }

    @Override
    public RpcCallback removeCallback(long id) {
        if (side().equals(ChannelSide.SERVER)) {
            throw new StarlightRpcException(
                "RpcChannel side is SERVER, not support removeCallback. " + "RequestId: " + id);
        }
        return callbacks.remove(id);
    }

    @Override
    public void close() {
        if (!inited.getAndSet(false)) { // not inited
            return;
        }

        // clear callback
        if (callbacks.size() > 0) {
            LOGGER.warn(
                "There are still unfinished requests when RpcChannel close, size {}, channelId {}, "
                    + "remoteAddress {}, will wait and handle this requests.",
                callbacks.size(), channel.id().asLongText(), channel.remoteAddress());
            clearCallbacks();
        }

        // remove old channel related classloader from LocalContext
        LocalContext.getContext(Constants.LOCAL_CONTEXT_THREAD_CLASSLOADER_KEY).set(channel.id().asLongText(), null);

        if (channel != null) {
            LOGGER.info("The netty channel is closing, channel {}, remoteAddress {}", channel.id().asLongText(),
                channel.remoteAddress());
            channel.close();
        }
    }

    @Override
    public void setAttribute(String attributeKey, Object attributeVal) {
        attributes.put(attributeKey, attributeVal);
    }

    @Override
    public Object getAttribute(String attributeKey) {
        return attributes.get(attributeKey);
    }

    @Override
    public Object removeAttribute(String attributeKey) {
        return attributes.remove(attributeKey);
    }

    private boolean isHttpShortConnection(MsgBase msgBase) {
        if (msgBase == null) {
            return false;
        }

        if (!(msgBase instanceof Response)) {
            return false;
        }

        Response response = (Response) msgBase;
        if (response.getRequest().getAttachmentKv() == null || response.getRequest().getAttachmentKv().size() == 0) {
            return false;
        }

        Object connection = response.getRequest().getAttachmentKv().get(HttpHeaderNames.CONNECTION.toString());
        if (!(connection instanceof String)) {
            return false;
        }

        return ((String) connection).equalsIgnoreCase(HttpHeaderValues.CLOSE.toString());
    }

    @Override
    public String toString() {
        return channel.id().asLongText();
    }

    @Override
    public RpcChannelGroup getRpcChannelGroup() {
        return this.channelGroup;
    }

    @Override
    public Map<Long, RpcCallback> allCallbacks() {
        return this.callbacks;
    }

    /**
     * Will be called when close channel, to prevent unhandled callback
     */
    protected void clearCallbacks() {
        long startClearTime = System.currentTimeMillis();

        for (;;) {
            if (callbacks.size() <= 0) {
                LOGGER.info("The channel has handled all request, will close, channelId {}, remoteAddr {}",
                    channel.id().asLongText(), channel.remoteAddress());
                break;
            }

            // FIXME 兜底方案，实际场景会走不到，因为每个请求对应的callback均有超时时间
            if ((System.currentTimeMillis() - startClearTime) > (1000 * 60 * 3)) { // max wait 3 min
                LOGGER.error("The request has not been processed after waiting 3 minutes when closing channel. "
                    + "Unhandled request size {}, will response timeout", callbacks.size());
                for (RpcCallback callback : callbacks.values()) {
                    callback.onError(StarlightRpcException.timeoutException(callback.getRequest(),
                        getRemoteAddress().getAddress().getHostAddress() + ":" + getRemoteAddress().getPort()));
                }
                callbacks.clear();

                break;
            }

            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                LOGGER.error(
                    "Thread interrupted when clearCallbacks, remind callback size {}, " + "will response timeout",
                    callbacks.size());
                // ignore
            }
        }
    }
}
