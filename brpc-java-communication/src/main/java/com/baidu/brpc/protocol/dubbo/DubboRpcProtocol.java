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
package com.baidu.brpc.protocol.dubbo;

import com.alibaba.com.caucho.hessian.io.Hessian2Input;
import com.baidu.brpc.ChannelInfo;
import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.buffer.DynamicCompositeByteBuf;
import com.baidu.brpc.client.RpcFuture;
import com.baidu.brpc.exceptions.BadSchemaException;
import com.baidu.brpc.exceptions.NotEnoughDataException;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.exceptions.TooBigDataException;
import com.baidu.brpc.protocol.*;
import com.baidu.brpc.server.ServiceManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * dubbo protocol with hession2 serialization
 */
@Slf4j
public class DubboRpcProtocol extends AbstractProtocol {
    protected static final byte FLAG_REQUEST = (byte) 0x80;
    protected static final byte FLAG_TWOWAY = (byte) 0x40;
    protected static final byte FLAG_EVENT = (byte) 0x20;

    private static final NotEnoughDataException notEnoughDataException
            = new NotEnoughDataException("not enough data");

    private ServiceManager serviceManager = ServiceManager.getInstance();

    @Override
    public Object decode(ChannelHandlerContext ctx, DynamicCompositeByteBuf in, boolean isDecodingRequest)
            throws TooBigDataException, NotEnoughDataException, BadSchemaException {
        if (in.readableBytes() < DubboConstants.FIXED_HEAD_LEN) {
            throw notEnoughDataException;
        }
        ByteBuf headerBuf = in.retainedSlice(DubboConstants.FIXED_HEAD_LEN);
        try {
            DubboHeader dubboHeader = DubboHeader.decode(headerBuf);
            if (dubboHeader.getMagic() != DubboConstants.MAGIC) {
                throw new BadSchemaException("not valid magic head for dubbo");
            }
            // 512M
            if (dubboHeader.getBodyLength() > 512 * 1024 * 1024) {
                throw new TooBigDataException("dubbo too big body size:" + dubboHeader.getBodyLength());
            }

            if (in.readableBytes() < dubboHeader.getBodyLength() + DubboConstants.FIXED_HEAD_LEN) {
                throw notEnoughDataException;
            }

            in.skipBytes(DubboConstants.FIXED_HEAD_LEN);
            ByteBuf bodyBuf = in.readRetainedSlice(dubboHeader.getBodyLength());

            DubboPacket dubboPacket = new DubboPacket();
            dubboPacket.setHeader(dubboHeader);
            dubboPacket.setBodyBuf(bodyBuf);
            return dubboPacket;
        } finally {
            headerBuf.release();
        }
    }

    @Override
    public ByteBuf encodeRequest(Request request) throws Exception {
        DubboHeader header = new DubboHeader();
        byte flag = (byte) (FLAG_REQUEST | getContentTypeId());
        if (!request.isOneWay()) {
            flag |= FLAG_TWOWAY;
        }
        if (request.isHeartbeat()) {
            flag |= FLAG_EVENT;
        }
        header.setFlag(flag);
        header.setCorrelationId(request.getCorrelationId());

        byte[] bodyBytes = null;
        if (request.isHeartbeat()) {
            bodyBytes = DubboPacket.encodeHeartbeatBody();
        } else {
            DubboRequestBody requestBody = new DubboRequestBody();
            requestBody.setPath(request.getServiceName());
            requestBody.setVersion(request.getSubscribeInfo().getVersion());
            requestBody.setMethodName(request.getMethodName());
            requestBody.setParameterTypes(request.getTargetMethod().getParameterTypes());
            requestBody.setArguments(request.getArgs());

            Map<String, String> kvAttachments = new HashMap<String, String>();
            kvAttachments.put("group", request.getSubscribeInfo().getGroup());
            if (request.getKvAttachment() != null) {
                for (Map.Entry<String, Object> entry : request.getKvAttachment().entrySet()) {
                    kvAttachments.put(entry.getKey(), (String) entry.getValue());
                }
            }
            requestBody.setAttachments(kvAttachments);
            bodyBytes = requestBody.encodeRequestBody();
        }
        header.setBodyLength(bodyBytes.length);

        return Unpooled.wrappedBuffer(header.encode(), Unpooled.wrappedBuffer(bodyBytes));
    }

    @Override
    public Response decodeResponse(Object msg, ChannelHandlerContext ctx) throws Exception {
        Response response = new RpcResponse();
        DubboPacket dubboPacket = (DubboPacket) msg;
        DubboHeader dubboHeader = dubboPacket.getHeader();
        // correlationId & rpc future
        response.setCorrelationId(dubboHeader.getCorrelationId());
        ChannelInfo channelInfo = ChannelInfo.getClientChannelInfo(ctx.channel());
        RpcFuture future = channelInfo.removeRpcFuture(response.getCorrelationId());
        if (future == null) {
            return response;
        }
        response.setRpcFuture(future);
        // status
        byte status = dubboHeader.getStatus();
        if (status == DubboConstants.RESPONSE_OK) {
            if ((dubboHeader.getFlag() & DubboConstants.FLAG_EVENT) != 0) {
                Object bodyObject = DubboPacket.decodeEventBody(dubboPacket.getBodyBuf());
                if (bodyObject == DubboConstants.HEARTBEAT_EVENT) {
                    response.setHeartbeat(true);
                } else {
                    throw new RpcException("response body not null for event");
                }
            } else {
                DubboResponseBody responseBody = DubboResponseBody.decodeResponseBody(dubboPacket.getBodyBuf());
                response.setResult(responseBody.getResult());
                response.setException(responseBody.getException());
                if (responseBody.getAttachments() != null) {
                    Map<String, Object> attachments = new HashMap<String, Object>();
                    for (Map.Entry<String, String> entry : responseBody.getAttachments().entrySet()) {
                        attachments.put(entry.getKey(), entry.getValue());
                    }
                    response.setKvAttachment(attachments);
                }
            }
        } else {
            ByteBufInputStream inputStream = null;
            try {
                inputStream = new ByteBufInputStream(dubboPacket.getBodyBuf(), true);
                Hessian2Input hessian2Input = new Hessian2Input(inputStream);
                String errorString = hessian2Input.readString();
                response.setException(new RpcException(RpcException.SERVICE_EXCEPTION, errorString));
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        }
        return response;
    }

    @Override
    public Request decodeRequest(Object packet) throws Exception {
        Request request = new RpcRequest();
        DubboPacket dubboPacket = (DubboPacket) packet;
        request.setCorrelationId(dubboPacket.getHeader().getCorrelationId());

        // check if it is heartbeat request
        byte flag = dubboPacket.getHeader().getFlag();
        if ((flag & DubboConstants.FLAG_EVENT) != 0) {
            Object bodyObject = DubboPacket.decodeEventBody(dubboPacket.getBodyBuf());
            if (bodyObject == DubboConstants.HEARTBEAT_EVENT) {
                request.setHeartbeat(true);
            } else {
                throw new RpcException("request body not null for event");
            }
            return request;
        }

        try {
            DubboRequestBody dubboRequestBody = DubboRequestBody.decodeRequestBody(dubboPacket.getBodyBuf());
            request.setArgs(dubboRequestBody.getArguments());
            request.setMethodName(dubboRequestBody.getPath());
            request.setRpcMethodInfo(dubboRequestBody.getRpcMethodInfo());
            request.setTarget(dubboRequestBody.getRpcMethodInfo().getTarget());
            request.setTargetMethod(dubboRequestBody.getRpcMethodInfo().getMethod());
            if (dubboRequestBody.getAttachments().size() > 0) {
                Map<String, Object> attachments = new HashMap<String, Object>(
                        dubboRequestBody.getAttachments().size());
                for (Map.Entry<String, String> entry : dubboRequestBody.getAttachments().entrySet()) {
                    attachments.put(entry.getKey(), entry.getValue());
                }
                request.setKvAttachment(attachments);
            }
            return request;
        } catch (Exception e) {
            log.error("dubbo decodeRequest error at {} ", e.getMessage(), e);
            throw new RpcException("dubbo decodeRequest error", e);
        }
    }

    @Override
    public ByteBuf encodeResponse(Request request, Response response) throws Exception {
        try {
            DubboHeader dubboHeader = new DubboHeader();
            DubboResponseBody responseBody = new DubboResponseBody();
            dubboHeader.setFlag(getContentTypeId());
            if (request.isHeartbeat()) {
                dubboHeader.setFlag((byte) (dubboHeader.getFlag() | FLAG_EVENT));
            }
            dubboHeader.setCorrelationId(response.getCorrelationId());
            if (response.getException() != null) {
                dubboHeader.setStatus(DubboConstants.SERVICE_ERROR);
                byte[] bodyBytes = DubboResponseBody.encodeErrorResponseBody(response.getException().getMessage());
                dubboHeader.setBodyLength(bodyBytes.length);
                return Unpooled.wrappedBuffer(dubboHeader.encode(), Unpooled.wrappedBuffer(bodyBytes));
            } else {
                dubboHeader.setStatus(DubboConstants.RESPONSE_OK);
                if (request.isHeartbeat()) {
                    byte[] bodyBytes = DubboResponseBody.encodeHeartbeatResponseBody();
                    dubboHeader.setBodyLength(bodyBytes.length);
                    return Unpooled.wrappedBuffer(dubboHeader.encode(), Unpooled.wrappedBuffer(bodyBytes));
                } else {
                    responseBody.setResult(response.getResult());
                    if (response.getKvAttachment() != null && response.getKvAttachment().size() > 0) {
                        responseBody.setResponseType(DubboConstants.RESPONSE_VALUE_WITH_ATTACHMENTS);
                        Map<String, String> attachments = new HashMap<String, String>();
                        for (Map.Entry<String, Object> entry : response.getKvAttachment().entrySet()) {
                            attachments.put(entry.getKey(), (String) entry.getValue());
                        }
                        responseBody.setAttachments(attachments);
                    } else {
                        responseBody.setResponseType(DubboConstants.RESPONSE_VALUE);
                    }
                    byte[] bodyBytes = responseBody.encodeResponseBody();
                    dubboHeader.setBodyLength(bodyBytes.length);
                    return Unpooled.wrappedBuffer(dubboHeader.encode(), Unpooled.wrappedBuffer(bodyBytes));
                }
            }
        } catch (Exception e) {
            log.warn("encode response failed", e);
            throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e);
        }
    }

    @Override
    public boolean supportHeartbeat() {
        return true;
    }

    protected byte getContentTypeId() {
        return DubboConstants.HESSIAN2_SERIALIZATION_ID;
    }
}
