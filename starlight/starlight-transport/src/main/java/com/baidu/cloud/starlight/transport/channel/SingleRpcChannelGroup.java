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
import com.baidu.cloud.starlight.api.common.URI;
import com.baidu.cloud.starlight.api.exception.TransportException;
import com.baidu.cloud.starlight.api.rpc.LocalContext;
import com.baidu.cloud.starlight.api.transport.channel.ChannelSide;
import com.baidu.cloud.starlight.api.transport.channel.RpcChannel;
import com.baidu.cloud.starlight.transport.utils.TimerHolder;
import com.baidu.cloud.thirdparty.netty.bootstrap.Bootstrap;
import com.baidu.cloud.thirdparty.netty.channel.Channel;
import com.baidu.cloud.thirdparty.netty.util.Timeout;
import com.baidu.cloud.thirdparty.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Single long RpcChannel group. Created by liuruisen on 2020/11/25.
 */
public class SingleRpcChannelGroup extends NettyRpcChannelGroup {

    private static final Integer CHANNEL_NUM = 1;

    private RpcChannel rpcChannel;

    private AtomicBoolean inited = new AtomicBoolean(false);

    private Set<Timeout> reconnectTimeouts;

    public SingleRpcChannelGroup(URI uri, Bootstrap bootStrap) {
        super(uri, bootStrap);
        reconnectTimeouts = new CopyOnWriteArraySet<>();
        // init();
    }

    @Override
    public void init() {
        if (inited.compareAndSet(false, true)) {
            Channel channel = connect();
            rpcChannel = new LongRpcChannel(channel, ChannelSide.CLIENT, this);
            // cache protocol information, so we can use it in DecoderHandler
            String protocolName = getUri().getProtocol();
            rpcChannel.setAttribute(Constants.PROTOCOL_ATTR_KEY, protocolName);
        }
    }

    @Override
    public RpcChannel getRpcChannel() throws TransportException {
        if (rpcChannel == null || !rpcChannel.isActive()) {
            synchronized (this) {
                LOGGER.debug("SingleChannelGroup getChannel start");
                if (rpcChannel == null || !rpcChannel.isActive()) {
                    LOGGER.info("Get RpcChannel from SingleRpcChannelGroup, the original RpcChannel is not active. "
                        + "Will create new, remoteAddress {}, rpcChannel {}.", getUri().getAddress(), rpcChannel);
                    if (rpcChannel != null) {
                        LOGGER.info("Close old rpcChannel when create new, old RpcChannel {}," + "RemoteAddress {}",
                            rpcChannel, getUri().getAddress());
                        // close old channel, use thread to reduce time consuming
                        new Thread(rpcChannel::close).start();
                    }
                    // create new channel
                    RpcChannel newChannel = new LongRpcChannel(connect(), ChannelSide.CLIENT);
                    newChannel.setAttribute(Constants.PROTOCOL_ATTR_KEY, getUri().getProtocol());
                    returnRpcChannel(newChannel);
                }
            }
        }
        LocalContext.getContext(Constants.LOCAL_CONTEXT_THREAD_CLASSLOADER_KEY)
            .set(rpcChannel.channel().id().asLongText(), Thread.currentThread().getContextClassLoader());
        return rpcChannel;
    }

    /**
     * When return to single long channel group, it means check and update the old RpcChannel
     *
     * @param rpcChannel
     */
    @Override
    public void returnRpcChannel(RpcChannel rpcChannel) {
        if (rpcChannel != this.rpcChannel) {
            synchronized (this) {
                if (rpcChannel != this.rpcChannel) {
                    this.rpcChannel = rpcChannel;
                }
            }
        }
    }

    // 当前仅在HeartBeatHandler与RpcHandler中调用，加上根据Netty的Channel与EventLoop映射关系，能保证单线程执行
    @Override
    public void removeRpcChannel(RpcChannel rpcChannel) {
        if (rpcChannel != this.rpcChannel) { // there is a newer rpcChannel, not need remove
            return;
        }

        // reconnect
        Timeout timeout =
            TimerHolder.getTimer().newTimeout(new ReconnectTask(this, rpcChannel), 0, TimeUnit.MILLISECONDS);
        reconnectTimeouts.add(timeout);
    }

    @Override
    public int rpcChannelCount() {
        return CHANNEL_NUM;
    }

    @Override
    public void close() {
        for (Timeout timeout : reconnectTimeouts) {
            timeout.cancel();
        }
        if (rpcChannel != null) {
            rpcChannel.close();
        }
    }

    @Override
    public Set<RpcChannel> allRpcChannels() {
        Set<RpcChannel> rpcChannels = new HashSet<>();
        rpcChannels.add(rpcChannel);
        return rpcChannels;
    }

    public Set<Timeout> getReconnectTimeouts() {
        return reconnectTimeouts;
    }

    /**
     * ReconnectTask will execute reconnect
     */
    protected static class ReconnectTask implements TimerTask {
        private static final Logger LOGGER = LoggerFactory.getLogger(ReconnectTask.class);

        private final SingleRpcChannelGroup channelGroup;

        private final RpcChannel needReconChannel;

        public ReconnectTask(SingleRpcChannelGroup singleRpcChannelGroup, RpcChannel needReconChannel) {
            this.channelGroup = singleRpcChannelGroup;
            this.needReconChannel = needReconChannel;
        }

        @Override
        public void run(Timeout timeout) throws Exception {

            // Prevent concurrency problems with other threads, such as user request thread
            synchronized (channelGroup) {
                try {
                    LOGGER.debug("SingleChannelGroup reconnect start");
                    // create new channel and close old channel
                    RpcChannel newChannel = channelGroup.getRpcChannel();
                    if (newChannel != needReconChannel) {
                        LOGGER.info(
                            "Reconnect rpc channel success, channel not same, not need reconnect, "
                                + "need connect channel {}, new channel {}, remoteAddress {}",
                            needReconChannel, newChannel, channelGroup.getRpcChannel().getRemoteAddress());
                        return;
                    }
                    // use for no heartbeat protocol?
                    LOGGER.info("Will close and reconnect channel {}, remoteAddress {}", needReconChannel,
                        needReconChannel.getRemoteAddress());

                    // close old channel, use thread to reduce time consuming
                    new Thread(needReconChannel::close).start();

                    newChannel = new LongRpcChannel(channelGroup.connect(), ChannelSide.CLIENT, channelGroup);
                    channelGroup.returnRpcChannel(newChannel);
                    LOGGER.info("Reconnect rpc channel success, remoteAddress {}", newChannel.getRemoteAddress());
                } catch (TransportException e) {
                    Integer reconnectedTimes = (Integer) needReconChannel.getAttribute(Constants.RECONNECTED_TIMES_KEY);
                    if (reconnectedTimes == null) {
                        reconnectedTimes = 0;
                    }
                    if (reconnectedTimes >= Constants.MAX_RECONNECT_TIMES) {
                        LOGGER.info(
                            "Maximum number {} of connection retries reached, reconnect failed: "
                                + "remoteHost {}, remotePort {}",
                            channelGroup.getUri().getHost(), channelGroup.getUri().getPort(), e);
                        return;
                    }
                    needReconChannel.setAttribute(Constants.RECONNECTED_TIMES_KEY, ++reconnectedTimes);
                    LOGGER.info("Reconnect to remote {}:{} failed, retry times {}, will retry after {}s",
                        channelGroup.getUri().getHost(), channelGroup.getUri().getPort(), reconnectedTimes,
                        Constants.RECONNECT_RETRY_INTERVAL_AFTER_FAILED);
                    // return after 10s
                    Timeout timeout1 = timeout.timer().newTimeout(timeout.task(),
                        Constants.RECONNECT_RETRY_INTERVAL_AFTER_FAILED, TimeUnit.SECONDS);
                    channelGroup.getReconnectTimeouts().add(timeout1);
                } finally {
                    channelGroup.getReconnectTimeouts().remove(timeout);
                }
            }
        }
    }
}
