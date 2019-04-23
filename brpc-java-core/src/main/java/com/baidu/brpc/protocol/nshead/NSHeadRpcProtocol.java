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

import java.io.IOException;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import com.baidu.brpc.protocol.Options;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import com.baidu.brpc.protocol.RpcRequest;
import com.baidu.brpc.protocol.RpcResponse;
import com.baidu.brpc.server.ServiceManager;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

/**
 * 处理nshead协议，序列化使用protobuf
 */
@SuppressWarnings("unchecked")
public class NSHeadRpcProtocol extends AbstractProtocol {
    private static final Logger LOG = LoggerFactory.getLogger(NSHeadRpcProtocol.class);
    private static final Gson gson = (new GsonBuilder())
            .disableHtmlEscaping()
            .serializeSpecialFloatingPointValues()
            .create();
    private int protocol;
    private String encoding = "utf-8";

    public NSHeadRpcProtocol(int protocol, String encoding) {
        this.protocol = protocol;
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
        Long logId = channelInfo.getLogId();
        if (packet.getNsHead().logId != 0) {
            logId = (long) packet.getNsHead().logId;
        }
        rpcResponse.setLogId(logId);
        RpcFuture future = channelInfo.removeRpcFuture(rpcResponse.getLogId());
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

    private byte[] encodeBody(Object body, RpcMethodInfo rpcMethodInfo) {
        Validate.notNull(body, "body must not be empty");

        byte[] bytes;
        if (protocol == Options.ProtocolType.PROTOCOL_NSHEAD_PROTOBUF_VALUE) {
            try {
                if (rpcMethodInfo.getTarget() != null) {
                    // server端，所以是encode response
                    bytes = rpcMethodInfo.outputEncode(body);
                } else {
                    bytes = rpcMethodInfo.inputEncode(body);
                }
            } catch (IOException ex) {
                throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, ex);
            }
        } else if (protocol == Options.ProtocolType.PROTOCOL_NSHEAD_JSON_VALUE) {
            String json = gson.toJson(body);
            try {
                bytes = json.getBytes(this.encoding);
            } catch (Exception e) {
                LOG.error("can not serialize object using mcpack", e);
                throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e);
            }
        } else {
            throw new RpcException(RpcException.SERVICE_EXCEPTION, "NSHeadRpcProtocol do not support " + protocol);
        }

        return bytes;
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

    private Object decodeBody(ByteBuf bodyBuf, RpcMethodInfo rpcMethodInfo) {
        try {
            Object result;
            if (protocol == Options.ProtocolType.PROTOCOL_NSHEAD_PROTOBUF_VALUE) {
                try {
                    if (rpcMethodInfo.getTarget() != null) {
                        // server端，所以是decode request
                        result = rpcMethodInfo.inputDecode(bodyBuf);
                    } else {
                        result = rpcMethodInfo.outputDecode(bodyBuf);
                    }
                } catch (IOException e) {
                    LOG.warn("invoke parseFrom method error", e);
                    throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e);
                }
            } else if (protocol == Options.ProtocolType.PROTOCOL_NSHEAD_JSON_VALUE) {
                try {
                    int readableBytes = bodyBuf.readableBytes();
                    byte[] bodyBytes = new byte[readableBytes];
                    bodyBuf.readBytes(bodyBytes);
                    String jsonString = new String(bodyBytes, this.encoding);
                    if (rpcMethodInfo.getTarget() != null) {
                        // server端
                        result = gson.fromJson(jsonString, rpcMethodInfo.getInputClasses()[0]);
                    } else {
                        result = gson.fromJson(jsonString, rpcMethodInfo.getOutputClass());
                    }
                } catch (Exception e) {
                    LOG.error("can not deserialize object", e);
                    throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e);
                }
            } else {
                throw new RpcException(RpcException.SERVICE_EXCEPTION, "NSHeadRpcProtocol do not support " + protocol);
            }
            return result;
        } finally {
            if (bodyBuf != null) {
                bodyBuf.release();
            }
        }
    }
}
