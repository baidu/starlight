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

package com.baidu.brpc.protocol.nshead;

import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.brpc.ChannelInfo;
import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.buffer.DynamicCompositeByteBuf;
import com.baidu.brpc.client.RpcFuture;
import com.baidu.brpc.exceptions.BadSchemaException;
import com.baidu.brpc.exceptions.NotEnoughDataException;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.exceptions.TooBigDataException;
import com.baidu.brpc.protocol.AbstractProtocol;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import com.baidu.brpc.protocol.RpcResponse;
import com.baidu.brpc.server.ServiceManager;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

/**
 * nshead based protocol, the header format is {@link NSHead}
 */
@SuppressWarnings("unchecked")
public abstract class NSHeadRpcProtocol extends AbstractProtocol {
    private static final Logger LOG = LoggerFactory.getLogger(NSHeadRpcProtocol.class);
    protected String encoding = "utf-8";

    public NSHeadRpcProtocol(String encoding) {
        if (encoding != null) {
            this.encoding = encoding;
        }
    }

    @Override
    public ByteBuf encodeRequest(Request request) throws Exception {
        Validate.notEmpty(request.getArgs(), "args must not be empty");
        byte[] bodyBytes = encodeBody(request.getArgs()[0], request.getRpcMethodInfo());
        NSHead nsHead = request.getNsHead();
        nsHead.bodyLength = bodyBytes.length;
        byte[] nsHeadBytes = nsHead.toBytes();
        return Unpooled.wrappedBuffer(nsHeadBytes, bodyBytes);
    }

    @Override
    public Response decodeResponse(Object in, ChannelHandlerContext ctx) throws Exception {
        NSHeadPacket packet = (NSHeadPacket) in;
        RpcResponse rpcResponse = new RpcResponse();
        ChannelInfo channelInfo = ChannelInfo.getClientChannelInfo(ctx.channel());
        Long correlationId = channelInfo.getCorrelationId();
        rpcResponse.setCorrelationId(correlationId);
        RpcFuture future = channelInfo.removeRpcFuture(rpcResponse.getCorrelationId());
        if (future == null) {
            return rpcResponse;
        }
        rpcResponse.setRpcFuture(future);

        Object responseBody = decodeBody(packet.getBodyBuf(), future.getRpcMethodInfo());
        if (responseBody == null) {
            return null;
        }

        rpcResponse.setResult(responseBody);
        return rpcResponse;
    }

    @Override
    public ByteBuf encodeResponse(Request request, Response response) throws Exception {
        byte[] bodyBytes = encodeBody(response.getResult(), response.getRpcMethodInfo());
        NSHead nsHead = new NSHead((int) response.getLogId(), bodyBytes.length);
        byte[] headBytes = nsHead.toBytes();
        return Unpooled.wrappedBuffer(headBytes, bodyBytes);
    }

    @Override
    public Request decodeRequest(Object packet) throws Exception {
        Request request = this.createRequest();
        NSHeadPacket nsHeadPacket = (NSHeadPacket) packet;
        request.setLogId((long) nsHeadPacket.getNsHead().logId);

        ServiceManager serviceManager = ServiceManager.getInstance();
        Map<String, RpcMethodInfo> serviceInfoMap = serviceManager.getServiceMap();
        if (serviceInfoMap.size() == 0) {
            String errMsg = "serviceInfoMap == 0";
            LOG.error(errMsg);
            request.setException(new RpcException(RpcException.SERVICE_EXCEPTION, errMsg));
            return request;
        }
        // 因为nshead的服务只会有一个接口，一个方法，所以这里只需要取第一个方法即可
        RpcMethodInfo rpcMethodInfo = serviceInfoMap.entrySet().iterator().next().getValue();
        if (rpcMethodInfo == null) {
            String errMsg = "serviceInfo is null in server";
            LOG.error(errMsg);
            request.setException(new RpcException(RpcException.SERVICE_EXCEPTION, errMsg));
            return request;
        }
        request.setServiceName(rpcMethodInfo.getServiceName());
        request.setMethodName(rpcMethodInfo.getMethodName());
        request.setRpcMethodInfo(rpcMethodInfo);

        Object body = decodeBody(nsHeadPacket.getBodyBuf(), rpcMethodInfo);
        request.setTarget(rpcMethodInfo.getTarget());
        request.setArgs(new Object[] {body});
        request.setTargetMethod(rpcMethodInfo.getMethod());
        return request;
    }

    @Override
    public boolean returnChannelBeforeResponse() {
        return false;
    }

    @Override
    public NSHeadPacket decode(ChannelHandlerContext ctx, DynamicCompositeByteBuf in, boolean isDecodingRequest)
            throws BadSchemaException, TooBigDataException, NotEnoughDataException {
        if (in.readableBytes() < NSHead.NSHEAD_LENGTH) {
            throw notEnoughDataException;
        }
        NSHeadPacket packet = new NSHeadPacket();
        ByteBuf fixHeaderBuf = in.retainedSlice(NSHead.NSHEAD_LENGTH);
        try {
            NSHead nsHead = NSHead.fromByteBuf(fixHeaderBuf);
            packet.setNsHead(nsHead);
            int bodyLength = nsHead.bodyLength;

            // 512M
            if (bodyLength > 512 * 1024 * 1024) {
                throw new TooBigDataException("to big body size:" + bodyLength);
            }

            if (in.readableBytes() < NSHead.NSHEAD_LENGTH + bodyLength) {
                throw notEnoughDataException;
            }

            in.skipBytes(NSHead.NSHEAD_LENGTH);
            ByteBuf bodyBuf = in.readRetainedSlice(bodyLength);
            packet.setBodyBuf(bodyBuf);
            return packet;
        } finally {
            fixHeaderBuf.release();
        }
    }

    public abstract byte[] encodeBody(Object body, RpcMethodInfo rpcMethodInfo);

    public abstract Object decodeBody(ByteBuf bodyBuf, RpcMethodInfo rpcMethodInfo);

}
