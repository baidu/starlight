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

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import org.apache.commons.collections.CollectionUtils;

import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.protocol.Protocol;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import com.baidu.brpc.protocol.RpcContext;
import com.baidu.brpc.protocol.http.HttpRpcProtocol;
import com.baidu.brpc.server.RpcServer;
import com.baidu.brpc.server.ServerStatus;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpUtil;
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
    private Object packet;
    private Protocol protocol;
    private ChannelHandlerContext ctx;

    @Override
    public void run() {

        Request request;
        Response response = protocol.getResponse();

        if (protocol instanceof HttpRpcProtocol) {
            FullHttpRequest fullHttpRequest = (FullHttpRequest) packet;
            if (fullHttpRequest.uri().equals("/favicon.ico")) {
                FullHttpResponse fullHttpResponse =
                        new DefaultFullHttpResponse(HTTP_1_1, OK);
                fullHttpResponse.headers().set(CONTENT_LENGTH, 0);
                if (HttpUtil.isKeepAlive(fullHttpRequest)) {
                    fullHttpResponse.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                }
                ChannelFuture f = ctx.channel().writeAndFlush(fullHttpResponse);
                if (!HttpUtil.isKeepAlive(fullHttpRequest)) {
                    f.addListener(ChannelFutureListener.CLOSE);
                }
                return;
            }
            if (fullHttpRequest.uri().equals("/") || fullHttpRequest.uri().equals("/status")) {
                ServerStatus serverStatus = rpcServer.getServerStatus();
                try {
                    byte[] statusBytes = serverStatus.toString().getBytes("UTF-8");
                    FullHttpResponse fullHttpResponse =
                            new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(statusBytes));
                    fullHttpResponse.headers().set(CONTENT_TYPE, "text/html");
                    fullHttpResponse.headers().set(CONTENT_LENGTH, fullHttpResponse.content().readableBytes());
                    if (HttpUtil.isKeepAlive(fullHttpRequest)) {
                        fullHttpResponse.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
                    }
                    ChannelFuture f = ctx.channel().writeAndFlush(fullHttpResponse);
                    if (!HttpUtil.isKeepAlive(fullHttpRequest)) {
                        f.addListener(ChannelFutureListener.CLOSE);
                    }
                } catch (Exception ex) {
                    log.warn("send status info response failed:", ex);
                }
                return;
            }
        }

        try {
            request = protocol.decodeRequest(packet);
        } catch (Exception ex) {
            // throw request
            log.warn("decode request failed:", ex);
            request = protocol.createRequest();
            request.setException(new RpcException(ex));
        }

        request.setChannel(ctx.channel());
        RpcContext rpcContext = RpcContext.getContext();

        try {
            rpcContext.setChannelForServer(ctx.channel());
            ByteBuf binaryAttachment = request.getBinaryAttachment();
            if (binaryAttachment != null) {
                rpcContext.setRequestBinaryAttachment(binaryAttachment);
            }

            response.setLogId(request.getLogId());
            response.setCompressType(request.getCompressType());
            response.setException(request.getException());
            response.setRpcMethodInfo(request.getRpcMethodInfo());

            // 处理请求前拦截
            if (response.getException() == null
                    && CollectionUtils.isNotEmpty(rpcServer.getInterceptors())) {
                for (Interceptor interceptor : rpcServer.getInterceptors()) {
                    if (!interceptor.handleRequest(request)) {
                        response.setException(new RpcException(
                                RpcException.FORBIDDEN_EXCEPTION, "intercepted"));
                        break;
                    }
                }
            }

            if (response.getException() == null) {
                try {
                    Object result = request.getTargetMethod().invoke(
                            request.getTarget(), request.getArgs()[0]);
                    response.setResult(result);
                    if (rpcContext.getResponseBinaryAttachment() != null
                            && rpcContext.getResponseBinaryAttachment().isReadable()) {
                        response.setBinaryAttachment(rpcContext.getResponseBinaryAttachment());
                    }
                } catch (Exception ex) {
                    String errorMsg = String.format("invoke method failed, msg=%s", ex.getMessage());
                    log.warn(errorMsg, ex);
                    response.setException(new RpcException(RpcException.SERVICE_EXCEPTION, errorMsg));
                }
            }

            // 处理响应后拦截
            if (CollectionUtils.isNotEmpty(rpcServer.getInterceptors())) {
                int length = rpcServer.getInterceptors().size();
                for (int i = length - 1; i >= 0; i--) {
                    rpcServer.getInterceptors().get(i).handleResponse(response);
                }
            }

            try {
                ByteBuf byteBuf = protocol.encodeResponse(request, response);
                ChannelFuture channelFuture = ctx.channel().writeAndFlush(byteBuf);
                protocol.afterResponseSent(request, response, channelFuture);
            } catch (Exception ex) {
                log.warn("send response failed:", ex);
            }
        } finally {
            rpcContext.reset();
        }
    }
}
