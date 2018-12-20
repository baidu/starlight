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

package com.baidu.brpc.client.handler;

import com.baidu.brpc.exceptions.BadSchemaException;
import com.baidu.brpc.exceptions.NotEnoughDataException;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.exceptions.TooBigDataException;
import com.baidu.brpc.ChannelInfo;
import com.baidu.brpc.client.RpcClient;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelHandler.Sharable
@Slf4j
public class RpcClientHandler extends SimpleChannelInboundHandler<Object> {
    private static final Logger LOG = LoggerFactory.getLogger(RpcClientHandler.class);

    private RpcClient rpcClient;

    public RpcClientHandler(RpcClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx,
                             Object in) throws Exception {
        if (!rpcClient.getRpcClientOptions().isHttp()) {
            ChannelInfo channelInfo = ChannelInfo.getClientChannelInfo(ctx.channel());
            ByteBuf msg = (ByteBuf) in;
            int len = msg.readableBytes();
            if (len > 0) {
                channelInfo.getRecvBuf().addBuffer(msg.retain());
            }

            ClientWorkTask[] tasks = new ClientWorkTask[64];
            int i = 0;
            while (channelInfo.getRecvBuf().readableBytes() > 0) {
                try {
                    Object packet = channelInfo.getProtocol().decode(channelInfo.getRecvBuf());
                    ClientWorkTask task = new ClientWorkTask(rpcClient, packet, channelInfo.getProtocol(), ctx);
                    tasks[i++] = task;
                    if (i == 64) {
                        rpcClient.getThreadPool().submit(tasks, 0, i);
                        i = 0;
                    }
                }  catch (NotEnoughDataException ex1) {
                    break;
                } catch (TooBigDataException ex2) {
                    throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, ex2);
                } catch (BadSchemaException ex3) {
                    throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, ex3);
                }
            }
            if (i > 0) {
                rpcClient.getThreadPool().submit(tasks, 0, i);
            }
        } else {
            FullHttpResponse response = (FullHttpResponse) in;
            // 这里要retain，因为handler调用完后，netty会release。
            response.retain();
            ClientWorkTask task = new ClientWorkTask(rpcClient, in, null, ctx);
            rpcClient.getThreadPool().submit(task);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        final ChannelInfo channelInfo = ChannelInfo.getClientChannelInfo(ctx.channel());

        if (channelInfo != null) {
            rpcClient.triggerCallback(new Runnable() {
                @Override
                public void run() {
                    String ip = channelInfo.getChannelGroup().getIp();
                    int port = channelInfo.getChannelGroup().getPort();
                    String errMsg = String.format("channel is non active, ip=%s,port=%d", ip, port);
                    RpcException ex = new RpcException(RpcException.NETWORK_EXCEPTION, errMsg);
                    channelInfo.handleChannelException(ex);
                }
            });
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        LOG.info(cause.getMessage());

        final ChannelInfo channelInfo = ChannelInfo.getClientChannelInfo(ctx.channel());
        if (channelInfo != null) {
            rpcClient.triggerCallback(new Runnable() {
                @Override
                public void run() {
                    RpcException ex = new RpcException(RpcException.SERIALIZATION_EXCEPTION, cause);
                    channelInfo.handleChannelException(ex);
                }
            });
        }
    }
}
