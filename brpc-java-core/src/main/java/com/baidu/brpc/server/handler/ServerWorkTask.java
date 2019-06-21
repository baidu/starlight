/*
 * Copyright (C) 2019 Baidu, Inc. All Rights Reserved.
 */

package com.baidu.brpc.server.handler;

import java.lang.reflect.InvocationTargetException;

import com.baidu.brpc.RpcContext;
import com.baidu.brpc.interceptor.DefaultInterceptorChain;
import com.baidu.brpc.interceptor.InterceptorChain;
import com.baidu.brpc.protocol.Protocol;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import com.baidu.brpc.server.RpcServer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
@Getter
@AllArgsConstructor
public class ServerWorkTask implements Runnable {
    private RpcServer rpcServer;
    private Protocol protocol;
    private Request request;
    private Response response;
    private ChannelHandlerContext ctx;

    @Override
    public void run() {
        RpcContext rpcContext = null;
        if (request != null) {
            request.setChannel(ctx.channel());
            if (request.getBinaryAttachment() != null
                    || request.getKvAttachment() != null) {
                rpcContext = RpcContext.getContext();
                if (request.getBinaryAttachment() != null) {
                    rpcContext.setRequestBinaryAttachment(request.getBinaryAttachment());
                }
                if (request.getKvAttachment() != null) {
                    rpcContext.setRequestKvAttachment(request.getKvAttachment());
                }
                rpcContext.setRemoteAddress(ctx.channel().remoteAddress());
            }

            response.setLogId(request.getLogId());
            response.setCompressType(request.getCompressType());
            response.setException(request.getException());
            response.setRpcMethodInfo(request.getRpcMethodInfo());
        }

        if (response.getException() == null) {
            try {
                InterceptorChain interceptorChain = new DefaultInterceptorChain(rpcServer.getInterceptors());
                interceptorChain.intercept(request, response);
                if (RpcContext.isSet()) {
                    rpcContext = RpcContext.getContext();
                    if (rpcContext.getResponseBinaryAttachment() != null
                            && rpcContext.getResponseBinaryAttachment().isReadable()) {
                        response.setBinaryAttachment(rpcContext.getResponseBinaryAttachment());
                    }
                    if (rpcContext.getResponseKvAttachment() != null
                            && !rpcContext.getResponseKvAttachment().isEmpty()) {
                        response.setKvAttachment(rpcContext.getResponseKvAttachment());
                    }
                }
            } catch (InvocationTargetException ex) {
                Throwable targetException = ex.getTargetException();
                if (targetException == null) {
                    targetException = ex;
                }
                String errorMsg = String.format("invoke method failed, msg=%s", targetException.getMessage());
                log.warn(errorMsg, targetException);
                response.setException(targetException);
            } catch (Throwable ex) {
                String errorMsg = String.format("invoke method failed, msg=%s", ex.getMessage());
                log.warn(errorMsg, ex);
                response.setException(ex);
            }
        }

        try {
            ByteBuf byteBuf = protocol.encodeResponse(request, response);
            ChannelFuture channelFuture = ctx.channel().writeAndFlush(byteBuf);
            protocol.afterResponseSent(request, response, channelFuture);
        } catch (Exception ex) {
            log.warn("send response failed:", ex);
        }

        if (rpcContext != null) {
            if (rpcContext.getRequestBinaryAttachment() != null
                    && rpcContext.getRequestBinaryAttachment().refCnt() > 0) {
                rpcContext.getRequestBinaryAttachment().release();
            }
            rpcContext.reset();
        }
    }
}
