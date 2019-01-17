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
import com.baidu.brpc.protocol.RpcRequest;
import com.baidu.brpc.protocol.RpcResponse;
import com.baidu.brpc.server.ServiceManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * 处理nshead协议，序列化使用protobuf
 */
@SuppressWarnings("unchecked")
public class NSHeadRpcProtocol extends AbstractProtocol<NSHeadBasePacket> {
    private static final Logger LOG = LoggerFactory.getLogger(NSHeadRpcProtocol.class);

    private int protocol;

    public NSHeadRpcProtocol(int protocol, String encoding) {
        this.protocol = protocol;
    }

    @Override
    public ByteBuf encodeRequest(RpcRequest request) throws Exception {
        Validate.notEmpty(request.getArgs(), "args must not be empty");

        byte[] bodyBytes = encodeBody(request.getArgs()[0], request.getRpcMethodInfo());
        NSHead nsHead;
        if (request.getNsHeadMeta() != null) {
            String provider = request.getNsHeadMeta().provider();
            short id = request.getNsHeadMeta().id();
            short version = request.getNsHeadMeta().version();
            nsHead = new NSHead((int) request.getLogId(), id, version, provider, bodyBytes.length);
        } else {
            nsHead = new NSHead((int) request.getLogId(), bodyBytes.length);
        }

        byte[] nsHeadBytes = nsHead.toBytes();
        return Unpooled.wrappedBuffer(nsHeadBytes, bodyBytes);
    }

    @Override
    public RpcResponse decodeResponse(NSHeadBasePacket packet, ChannelHandlerContext ctx) throws Exception {
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
    public ByteBuf encodeResponse(RpcResponse rpcResponse) throws Exception {
        byte[] bodyBytes = encodeBody(rpcResponse.getResult(), rpcResponse.getRpcMethodInfo());
        NSHead nsHead = new NSHead((int) rpcResponse.getLogId(), bodyBytes.length);
        byte[] headBytes = nsHead.toBytes();
        return Unpooled.wrappedBuffer(headBytes, bodyBytes);
    }

    @Override
    public void decodeRequest(Object in, RpcRequest request) throws Exception {
        NSHeadBasePacket packet = (NSHeadBasePacket) in;
        request.setLogId((long) packet.getNsHead().logId);

        ServiceManager serviceManager = ServiceManager.getInstance();
        Map<String, RpcMethodInfo> serviceInfoMap = serviceManager.getServiceMap();
        if (serviceInfoMap.size() == 0) {
            String errMsg = "serviceInfoMap == 0";
            LOG.error(errMsg);
            request.setException(new RpcException(RpcException.SERVICE_EXCEPTION, errMsg));
            return;
        }
        // 因为nshead的服务只会有一个接口，一个方法，所以这里只需要取第一个方法即可
        RpcMethodInfo rpcMethodInfo = serviceInfoMap.entrySet().iterator().next().getValue();
        if (rpcMethodInfo == null) {
            String errMsg = "serviceInfo is null in server";
            LOG.error(errMsg);
            request.setException(new RpcException(RpcException.SERVICE_EXCEPTION, errMsg));
            return;
        }
        request.setRpcMethodInfo(rpcMethodInfo);

        Object body = decodeBody(packet.getBodyBuf(), rpcMethodInfo);
        request.setTarget(rpcMethodInfo.getTarget());
        request.setArgs(new Object[]{body});
        request.setTargetMethod(rpcMethodInfo.getMethod());
        return;
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
        } else {
            throw new RpcException(RpcException.SERVICE_EXCEPTION, "NSHeadRpcProtocol do not support " + protocol);
        }

        return bytes;
    }

    @Override
    public NSHeadBasePacket decode(DynamicCompositeByteBuf in)
            throws BadSchemaException, TooBigDataException, NotEnoughDataException {
        if (in.readableBytes() < NSHead.NSHEAD_LENGTH) {
            throw new NotEnoughDataException("readable bytes less than 12 for nshead:" + in.readableBytes());
        }
        NSHeadBasePacket packet = new NSHeadBasePacket();
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
                String errMsg = String.format("readable bytes=%d, actualSize=%d",
                        in.readableBytes(), NSHead.NSHEAD_LENGTH + bodyLength);
                throw new NotEnoughDataException(errMsg);
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
