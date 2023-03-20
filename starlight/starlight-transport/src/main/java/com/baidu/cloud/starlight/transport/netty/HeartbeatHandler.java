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
import com.baidu.cloud.starlight.api.transport.channel.ChannelAttribute;
import com.baidu.cloud.starlight.api.transport.channel.ChannelSide;
import com.baidu.cloud.starlight.api.transport.channel.RpcChannel;
import com.baidu.cloud.starlight.api.protocol.HeartbeatTrigger;
import com.baidu.cloud.starlight.api.protocol.Protocol;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Check if the channel is Health, maintain long connection. ClientSide: When read idle detected send Heartbeat Msg.
 * ServerSide: When idle detected close Channel. Created by liuruisen on 2020/2/3.
 */
public class HeartbeatHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeartbeatHandler.class);

    /**
     * AttributeKey for HeartBeat Fail times
     **/
    public static final AttributeKey<Integer> HEARTBEAT_FAIL_TIMES = AttributeKey.valueOf("heartbeat");

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            ChannelAttribute attribute = ctx.channel().attr(RpcChannel.ATTRIBUTE_KEY).get();
            if (attribute.getRpcChannel().side().equals(ChannelSide.CLIENT)) { // client side: trigger heartbeat
                if (ctx.channel().attr(HEARTBEAT_FAIL_TIMES).get() == null) {
                    ctx.channel().attr(HEARTBEAT_FAIL_TIMES).set(0);
                }
                triggerHeartbeat(ctx);
            } else { // server side: close
                LOGGER.info("Server side, No IO operation for a long time, close the connection, remoteAddr {}",
                    ctx.channel().remoteAddress());
                ctx.channel().close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    private void triggerHeartbeat(ChannelHandlerContext context) {
        ChannelAttribute attribute = context.channel().attr(RpcChannel.ATTRIBUTE_KEY).get();
        if (attribute == null || attribute.getRpcChannel() == null) {
            throw new StarlightRpcException("AttributeKey<RpcChannel> value is null");
        }

        RpcChannel rpcChannel = attribute.getRpcChannel();
        HeartbeatTrigger heartbeatTrigger = null;
        if (attribute.getChannelProtocol() != null
            && !attribute.getChannelProtocol().equals(Constants.UNSPECIFIED_PROTOCOL)) {
            Protocol protocol =
                ExtensionLoader.getInstance(Protocol.class).getExtension(attribute.getChannelProtocol());
            heartbeatTrigger = protocol.getHeartbeatTrigger();
        }

        if (heartbeatTrigger != null) { // Protocol support heartbeat, send heartbeat msg
            Request heartbeatRequest = heartbeatTrigger.heartbeatRequest();
            ChannelFuture channelFuture = context.channel().writeAndFlush(heartbeatRequest);
            channelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if (!channelFuture.isSuccess()) { // send fail, add heartbeat fail times
                        addHeartbeatFailTimes(context.channel());
                        LOGGER.info("Send heartbeat Ping message to remote {} failed.", rpcChannel.getRemoteAddress(),
                            channelFuture.cause());
                    } else {
                        // send success, channel is alive, clear heartbeat fail times
                        // fixme 要不要移动到handleHeartbeatResponse
                        clearHeartbeatFailTimes(context.channel());
                    }
                }
            });
        } else { // Protocol not support heartbeat, add failTimes
            addHeartbeatFailTimes(context.channel());
        }

        // reconnect异常时，打印异常日志，不做处理，等待程序正常执行
        if (context.channel().attr(HEARTBEAT_FAIL_TIMES).get() >= Constants.MAX_HEARTBEAT_TIMES_VALUE) {
            try {
                rpcChannel.reconnect();
            } catch (Exception e) {
                LOGGER.debug("Heartbeat Reconnect Failed", e);
            }
        }
    }

    private void addHeartbeatFailTimes(Channel channel) {
        channel.attr(HEARTBEAT_FAIL_TIMES).set(channel.attr(HEARTBEAT_FAIL_TIMES).get() + 1);
    }

    private void clearHeartbeatFailTimes(Channel channel) {
        channel.attr(HEARTBEAT_FAIL_TIMES).set(0);
    }

    // handle heartbeat request and return heartbeat response
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ctx.channel().attr(HEARTBEAT_FAIL_TIMES).set(0);
        if (!(msg instanceof MsgBase)) {
            ctx.fireChannelRead(msg);
            return;
        }

        if (!((MsgBase) msg).isHeartbeat()) {
            ctx.fireChannelRead(msg);
            return;
        }

        ChannelAttribute attribute = ctx.channel().attr(RpcChannel.ATTRIBUTE_KEY).get();
        if (attribute == null || attribute.getRpcChannel() == null) {
            throw new TransportException(TransportException.RPC_CHANNEL_NULL_EXCEPTION,
                "RpcChannel in Channel is null");
        }

        RpcChannel rpcChannel = attribute.getRpcChannel();
        if (msg instanceof Request) { // heartbeat request
            handleHeartbeatRequest(rpcChannel, (Request) msg);
        }

        if (msg instanceof Response) { // heartbeat response
            handleHeartbeatResponse(rpcChannel, (Response) msg);
        }
    }

    private void handleHeartbeatRequest(RpcChannel rpcChannel, Request request) {
        // response pong message
        Protocol protocol = ExtensionLoader.getInstance(Protocol.class).getExtension(request.getProtocolName());
        if (protocol == null) {
            throw new TransportException(TransportException.HEARTBEAT_EXCEPTION,
                "Heartbeat request protocol is not supported");
        }
        Response response = protocol.getHeartbeatTrigger().heartbeatResponse();
        response.setId(request.getId());
        rpcChannel.send(response);
    }

    private void handleHeartbeatResponse(RpcChannel rpcChannel, Response response) {
        // do nothing
    }
}
