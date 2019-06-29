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

import java.lang.reflect.Method;

import com.baidu.brpc.RpcContext;
import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcFuture;
import com.baidu.brpc.protocol.Protocol;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import com.baidu.brpc.protocol.push.SPHead;
import com.baidu.brpc.protocol.push.ServerPushPacket;
import com.baidu.brpc.protocol.push.ServerPushProtocol;
import com.baidu.brpc.server.ServiceManager;

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
public class ClientWorkTask implements Runnable {
    private RpcClient rpcClient;
    private Object packet;
    private Protocol protocol;
    private ChannelHandlerContext ctx;

    @Override
    public void run() {
        // 只有server push协议下，有可能受到request类型
        if (protocol instanceof ServerPushProtocol) {
            // 区分类型
            SPHead spHead = ((ServerPushPacket) packet).getSpHead();
            if (spHead.getType() == SPHead.TYPE_PUSH_REQUEST) {
                handlePushRequest();
                return;
            }
        }

        Response response;
        try {
            response = protocol.decodeResponse(packet, ctx);
        } catch (Exception e) {
            log.warn("decode response failed:", e);
            return;
        }

        if (response.getRpcFuture() != null) {
            log.debug("handle response, logId={}", response.getLogId());
            RpcFuture future = response.getRpcFuture();
            future.handleResponse(response);
        } else {
            log.debug("rpcFuture is null, logId={}", response.getLogId());
        }
    }

    /**
     * 收到push请求的处理方法
     */
    private void handlePushRequest() {

        Request request = null;
        Response response = protocol.createResponse();
        try {
            request = protocol.decodeRequest(packet);
        } catch (Exception ex) {
            // throw request
            log.warn("decode request failed:", ex);
            response.setException(ex);
        } finally {
            if (request != null && request.getException() != null) {
                response.setException(request.getException());
            }
        }

        RpcContext rpcContext = null;
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

        String serviceName = request.getServiceName();
        String methodName = request.getMethodName();
        RpcMethodInfo service = ServiceManager.getInstance().getService(serviceName, methodName);
        Method targetMethod = request.getTargetMethod();
        Object t = service.getTarget();
        Object result = null;
        try {
            result = targetMethod.invoke(t, request.getArgs());
        } catch (Exception e) {
            log.error("exception :", e);
        }
        response.setResult(result);
        try {
            ByteBuf byteBuf = protocol.encodeResponse(request, response);
            ChannelFuture channelFuture = ctx.channel().writeAndFlush(byteBuf);
            protocol.afterResponseSent(request, response, channelFuture);
        } catch (Exception ex) {
            log.warn("send response failed:", ex);
        }

        if (rpcContext != null) {
            rpcContext.reset();
        }
    }

}
