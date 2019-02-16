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

import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.protocol.Protocol;
import com.baidu.brpc.protocol.RpcRequest;
import com.baidu.brpc.protocol.RpcResponse;
import com.baidu.brpc.protocol.http.HttpRpcProtocol;
import com.baidu.brpc.server.RpcServer;
import com.baidu.brpc.ChannelInfo;
import com.baidu.brpc.protocol.ProtocolManager;
import com.baidu.brpc.protocol.RpcContext;
import com.baidu.brpc.server.ServerStatus;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpHeaderNames;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@Slf4j
@Setter
@Getter
@AllArgsConstructor
public class ServerWorkTask implements Runnable {
    private RpcServer rpcServer;
    private Object packet;
    private Protocol protocol;
    private ChannelHandlerContext ctx;
    private boolean isHttp;
    private boolean isOpenMeta;

    @Override
    public void run() {
        RpcRequest rpcRequest = RpcRequest.getRpcRequest();
        rpcRequest.reset();
        RpcResponse rpcResponse = RpcResponse.getRpcResponse();
        rpcResponse.reset();
        if (!isHttp) {
            try {
                if (protocol != null) {
                    protocol.decodeRequest(packet, rpcRequest);
                }
            } catch (Exception ex) {
                // throw request
                log.warn("decode request failed:", ex);
                rpcRequest.setException(new RpcException(ex));
            }
        } else {
            FullHttpRequest fullHttpRequest = (FullHttpRequest) packet;
            if (fullHttpRequest.uri().equals("/favicon.ico")) {
                FullHttpResponse response =
                        new DefaultFullHttpResponse(HTTP_1_1, OK);
                response.headers().set(CONTENT_LENGTH, 0);
                if (HttpUtil.isKeepAlive(fullHttpRequest)) {
                    response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                }
                ChannelFuture f = ctx.channel().writeAndFlush(response);
                if (!HttpUtil.isKeepAlive(fullHttpRequest)) {
                    f.addListener(ChannelFutureListener.CLOSE);
                }
                return;
            }
            if (isOpenMeta
                    && (fullHttpRequest.uri().equals("/")
                    || fullHttpRequest.uri().equals("/status"))) {
                ServerStatus serverStatus = rpcServer.getServerStatus();
                try {
                    byte[] statusBytes = serverStatus.toString().getBytes("UTF-8");
                    FullHttpResponse response =
                            new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(statusBytes));
                    response.headers().set(CONTENT_TYPE, "text/html");
                    response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
                    if (HttpUtil.isKeepAlive(fullHttpRequest)) {
                        response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
                    }
                    ChannelFuture f = ctx.channel().writeAndFlush(response);
                    if (!HttpUtil.isKeepAlive(fullHttpRequest)) {
                        f.addListener(ChannelFutureListener.CLOSE);
                    }
                } catch (Exception ex) {
                    log.warn("send status info response failed:", ex);
                }
                return;
            }
            String contentTypeAndEncoding = fullHttpRequest.headers().get(HttpHeaderNames.CONTENT_TYPE);
            if (contentTypeAndEncoding != null) {
                contentTypeAndEncoding = contentTypeAndEncoding.toLowerCase();
                String[] splits = StringUtils.split(contentTypeAndEncoding, ";");
                int protocolType = HttpRpcProtocol.parseProtocolType(splits[0]);
                Protocol protocol = ProtocolManager.instance().getProtocol(protocolType);
                ChannelInfo channelInfo = ChannelInfo.getOrCreateServerChannelInfo(ctx.channel());
                channelInfo.setProtocol(protocol);
                this.protocol = protocol;
                protocol.decodeHttpRequest(fullHttpRequest, rpcRequest);
            } else {
                rpcRequest.setException(new RpcException(
                        RpcException.FORBIDDEN_EXCEPTION, "unknown content-type"));
                log.warn("content-type is null");
            }
        }
        rpcRequest.setChannel(ctx.channel());

        RpcContext rpcContext = RpcContext.getContext();

        try {
            rpcContext.setChannelForServer(ctx.channel());
            ByteBuf binaryAttachment = rpcRequest.getBinaryAttachment();
            if (binaryAttachment != null) {
                rpcContext.setRequestBinaryAttachment(binaryAttachment);
            }

            rpcResponse.setLogId(rpcRequest.getLogId());
            rpcResponse.setCompressType(rpcRequest.getCompressType());
            rpcResponse.setException(rpcRequest.getException());
            rpcResponse.setRpcMethodInfo(rpcRequest.getRpcMethodInfo());

            // 处理请求前拦截
            if (rpcResponse.getException() == null
                    && CollectionUtils.isNotEmpty(rpcServer.getInterceptors())) {
                for (Interceptor interceptor : rpcServer.getInterceptors()) {
                    if (!interceptor.handleRequest(rpcRequest)) {
                        rpcResponse.setException(new RpcException(
                                RpcException.FORBIDDEN_EXCEPTION, "intercepted"));
                        break;
                    }
                }
            }

            if (rpcResponse.getException() == null) {
                try {
                    Object result = rpcRequest.getTargetMethod().invoke(
                            rpcRequest.getTarget(), rpcRequest.getArgs()[0]);
                    rpcResponse.setResult(result);
                    if (rpcContext.getResponseBinaryAttachment() != null
                            && rpcContext.getResponseBinaryAttachment().isReadable()) {
                        rpcResponse.setBinaryAttachment(rpcContext.getResponseBinaryAttachment());
                    }
                } catch (Exception ex) {
                    String errorMsg = String.format("invoke method failed, msg=%s", ex.getMessage());
                    log.warn(errorMsg, ex);
                    rpcResponse.setException(new RpcException(RpcException.SERVICE_EXCEPTION, errorMsg));
                }
            }

            // 处理响应后拦截
            if (CollectionUtils.isNotEmpty(rpcServer.getInterceptors())) {
                int length = rpcServer.getInterceptors().size();
                for (int i = length - 1; i >= 0; i--) {
                    rpcServer.getInterceptors().get(i).handleResponse(rpcResponse);
                }
            }

            // http请求
            if (rpcServer.getRpcServerOptions().isHttp()) {
                FullHttpResponse httpResponse;
                if (protocol != null) {
                    httpResponse = protocol.encodeHttpResponse(rpcRequest, rpcResponse);
                } else {
                    httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                            HttpResponseStatus.INTERNAL_SERVER_ERROR);
                }
                ChannelFuture f = ctx.channel().writeAndFlush(httpResponse);
                if (!HttpUtil.isKeepAlive(rpcRequest)) {
                    f.addListener(ChannelFutureListener.CLOSE);
                }
            } else {
                try {
                    if (protocol != null) {
                        ByteBuf outBuf = protocol.encodeResponse(rpcResponse);
                        ctx.channel().writeAndFlush(outBuf);
                    }
                } catch (Exception ex) {
                    log.warn("send response failed:", ex);
                }
            }

        } finally {
            rpcContext.reset();
        }

    }
}
