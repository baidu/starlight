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

package com.baidu.brpc.server.handler;

import java.io.IOException;
import java.util.List;

import com.baidu.brpc.ChannelInfo;
import com.baidu.brpc.buffer.DynamicCompositeByteBuf;
import com.baidu.brpc.exceptions.BadSchemaException;
import com.baidu.brpc.exceptions.NotEnoughDataException;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.exceptions.TooBigDataException;
import com.baidu.brpc.protocol.Protocol;
import com.baidu.brpc.protocol.ProtocolManager;
import com.baidu.brpc.server.RpcServer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.unix.Errors;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by huwenwei on 2017/4/25.
 */
@ChannelHandler.Sharable
@Slf4j
public class RpcServerHandler extends SimpleChannelInboundHandler<Object> {

    private RpcServer rpcServer;

    public RpcServerHandler(RpcServer rpcServer) {
        this.rpcServer = rpcServer;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ChannelInfo channelInfo = ChannelInfo.getOrCreateServerChannelInfo(ctx.channel());
        channelInfo.setProtocol(rpcServer.getProtocol());
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object in) throws Exception {
        ChannelInfo channelInfo = ChannelInfo.getServerChannelInfo(ctx.channel());
        ByteBuf msg = (ByteBuf) in;
        int len = msg.readableBytes();
        if (len > 0) {
            channelInfo.getRecvBuf().addBuffer(msg.retain());
            DecodeWorkTask[] tasks = new DecodeWorkTask[64];
            int i = 0;
            while (channelInfo.getRecvBuf().readableBytes() > 0) {
                try {
                    Object packet = decodeHeader(ctx, channelInfo, channelInfo.getRecvBuf());
                    DecodeWorkTask task = new DecodeWorkTask(rpcServer, packet, channelInfo.getProtocol(), ctx);
                    tasks[i++] = task;
                    if (i == 64) {
                        rpcServer.getThreadPool().submit(tasks, 0, i);
                        i = 0;
                    }
                } catch (NotEnoughDataException ex1) {
                    break;
                } catch (TooBigDataException ex2) {
                    throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, ex2);
                } catch (BadSchemaException ex3) {
                    throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, ex3);
                }
            }
            if (i > 0) {
                rpcServer.getThreadPool().submit(tasks, 0, i);

            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (ctx.channel().isActive()
                && !(cause instanceof Errors.NativeIoException)
                && !(cause instanceof IOException)) {
            log.info("service exception, ex={}", cause.getMessage());
        }
        ctx.close();
    }

    /**
     * 尝试用各个协议解析header。
     * 目前所有的协议至少都需要12字节，所以第一个协议抛出not enough data异常后，就不重试剩余协议了。
     * 只要有一个协议抛too big data异常，就不再重试剩余协议。
     *
     * @param channelInfo      channel信息，包含protocol
     * @param compositeByteBuf 输入buffer
     *
     * @return 反序列化后packet
     *
     * @throws NotEnoughDataException
     * @throws TooBigDataException
     * @throws BadSchemaException
     */
    private Object decodeHeader(ChannelHandlerContext ctx,
                                ChannelInfo channelInfo,
                                DynamicCompositeByteBuf compositeByteBuf)
            throws NotEnoughDataException, TooBigDataException, BadSchemaException {
        Protocol protocol = channelInfo.getProtocol();
        if (protocol != null) {
            return protocol.decode(ctx, compositeByteBuf, true);
        }
        ProtocolManager protocolManager = ProtocolManager.instance();
        List<Protocol> protocols = protocolManager.getProtocols();
        int protocolNum = protocolManager.getProtocolNum();
        for (int i = 0; i < protocolNum; i++) {
            Protocol protocol1 = protocols.get(i);
            try {
                Object packet = protocol1.decode(ctx, compositeByteBuf, true);
                channelInfo.setProtocol(protocol1);
                return packet;
            } catch (BadSchemaException ex3) {
                // 遇到bad schema继续重试。
                continue;
            }
        }
        throw new BadSchemaException("bad schema");
    }
}
