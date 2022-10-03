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

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.exception.StarlightRpcException;
import com.baidu.cloud.starlight.api.exception.TransportException;
import com.baidu.cloud.starlight.api.extension.ExtensionLoader;
import com.baidu.cloud.starlight.api.model.MsgBase;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.model.RpcResponse;
import com.baidu.cloud.starlight.api.model.ShuttingDownEvent;
import com.baidu.cloud.starlight.api.protocol.Protocol;
import com.baidu.cloud.starlight.api.rpc.LocalContext;
import com.baidu.cloud.starlight.api.transport.ClientPeer;
import com.baidu.cloud.starlight.api.transport.Peer;
import com.baidu.cloud.starlight.api.transport.PeerStatus;
import com.baidu.cloud.starlight.api.transport.ServerPeer;
import com.baidu.cloud.starlight.api.transport.channel.ChannelSide;
import com.baidu.cloud.starlight.api.transport.channel.ThreadLocalChannelContext;
import com.baidu.cloud.starlight.protocol.stargate.StargateProtocol;
import com.baidu.cloud.starlight.transport.channel.LongRpcChannel;
import com.baidu.cloud.starlight.api.transport.channel.ChannelAttribute;
import com.baidu.cloud.starlight.api.transport.channel.RpcChannel;
import com.baidu.cloud.thirdparty.netty.channel.Channel;
import com.baidu.cloud.thirdparty.netty.channel.ChannelHandler;
import com.baidu.cloud.thirdparty.netty.channel.ChannelHandlerContext;
import com.baidu.cloud.thirdparty.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * Client Side: Handler Response Server Side: Handler Request Created by liuruisen on 2020/2/3.
 */
@ChannelHandler.Sharable
public class RpcHandler extends SimpleChannelInboundHandler<MsgBase> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcHandler.class);

    private final Peer peer;

    public RpcHandler(Peer peer) {
        this.peer = peer;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        ThreadLocalChannelContext.getContext().setChannel(channel);
        ChannelAttribute attribute = channel.attr(RpcChannel.ATTRIBUTE_KEY).get();
        if (attribute == null && peer instanceof ServerPeer) { // server side
            RpcChannel rpcChannel = new LongRpcChannel(channel, ChannelSide.SERVER);
            // cache channels
            ((ServerPeer) peer).rpcChannels().put(toAddressString((InetSocketAddress) channel.remoteAddress()),
                rpcChannel);
        }
        super.channelActive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MsgBase msg) throws Exception {
        if (msg == null) { // request or response
            throw new StarlightRpcException(StarlightRpcException.MSG_NULL_EXCEPTION,
                "The Message read from socket is null");
        }

        ChannelAttribute attribute = ctx.channel().attr(RpcChannel.ATTRIBUTE_KEY).get();
        // set feign rpc channel
        if (attribute == null || attribute.getRpcChannel() == null) { // server side
            throw new TransportException(TransportException.RPC_CHANNEL_NULL_EXCEPTION,
                "RpcChannel in Channel is null");
        }
        RpcChannel rpcChannel = attribute.getRpcChannel();
        rpcChannel.receive(msg); // Customized processing

        // client side: handle server SHUTTING_DOWN event
        if (msg instanceof Response && peer instanceof ClientPeer) {
            RpcResponse response = (RpcResponse) msg;
            if (response.getResult() instanceof ShuttingDownEvent) {
                LOGGER.info(
                    "Client receive server shutting down event, will close the related client, " + "remoteAddress {}",
                    rpcChannel.getRemoteAddress());
                peer.updateStatus(new PeerStatus(PeerStatus.Status.SHUTTING_DOWN, System.currentTimeMillis()));
                return;
            }
        }

        // server side: server is shutting down return shutting down response
        if ((peer.status().getStatus().equals(PeerStatus.Status.SHUTTING_DOWN)
            || peer.status().getStatus().equals(PeerStatus.Status.SHUTDOWN)) && msg instanceof Request
            && peer instanceof ServerPeer) {
            LOGGER.warn(
                "The server was shutting down and received a request, " + "status {}, remoteAddress {}, request {}",
                peer.status(), ctx.channel().remoteAddress(), msg);
            RpcResponse shuttingDownResponse = peer.shuttingDownResponse((Request) msg);
            Protocol protocol =
                ExtensionLoader.getInstance(Protocol.class).getExtension(StargateProtocol.PROTOCOL_NAME);
            protocol.getEncoder().encodeBody(shuttingDownResponse);

            ctx.channel().writeAndFlush(shuttingDownResponse);
            return;
        }

        peer.getProcessor().process(msg, rpcChannel);
    }

    /**
     * The {@link Channel} of the {@link ChannelHandlerContext} was registered is now inactive and reached its end of
     * lifetime.
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();

        if (peer instanceof ServerPeer) {
            LOGGER.info("Server Channel is closing, channelId {}, remoteAddress {}", channel.id().asLongText(),
                channel.remoteAddress());
            // remove cached channel
            ((ServerPeer) peer).rpcChannels().remove(toAddressString((InetSocketAddress) channel.remoteAddress()));
            return;
        }

        boolean isHeartbeatFail = false;
        if (channel.attr(HeartbeatHandler.HEARTBEAT_FAIL_TIMES).get() != null
            && channel.attr(HeartbeatHandler.HEARTBEAT_FAIL_TIMES).get() >= Constants.MAX_HEARTBEAT_TIMES_VALUE) {
            isHeartbeatFail = true;
        }
        // channel inactive because heartbeat fail will be reconnected through HeartbeatHandler,
        // so we do not reconnect here
        if (isHeartbeatFail) {
            LOGGER.info(
                "Client Channel is closing because heartbeat fail, will reconnect. " + "ChannelId {}, remoteAddress {}",
                channel.id().asLongText(), channel.remoteAddress());
            return;
        }

        ChannelAttribute attribute = channel.attr(RpcChannel.ATTRIBUTE_KEY).get();
        if (attribute != null && attribute.getRpcChannel() != null) {
            LOGGER.info(
                "Client Channel is being closing may cause by starlight call close "
                    + "| network unhealth | server closing. " + "ChannelId {}, remoteAddress {}",
                channel.id().asLongText(), channel.remoteAddress());
            // fixme Whether to reconnect
            // attribute.getRpcChannel().reconnect();
        }

        // remove old channel related classloader from LocalContext
        LocalContext.getContext(Constants.LOCAL_CONTEXT_THREAD_CLASSLOADER_KEY).set(channel.id().asLongText(), null);
        // super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // FIXME 如何正确处理编解码阶段抛出的异常，最好可以被client端感知
        // super.exceptionCaught(ctx, cause);
        LOGGER.debug("NettyHandlers unhandled exception appear, please pay attention. cause by {}", cause.getMessage());
    }

    private String toAddressString(InetSocketAddress address) {
        return address.getAddress().getHostAddress() + ":" + address.getPort();
    }
}
