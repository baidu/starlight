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

package com.baidu.brpc.protocol.hulu;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.brpc.ChannelInfo;
import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.buffer.DynamicCompositeByteBuf;
import com.baidu.brpc.client.RpcFuture;
import com.baidu.brpc.compress.Compress;
import com.baidu.brpc.compress.CompressManager;
import com.baidu.brpc.exceptions.BadSchemaException;
import com.baidu.brpc.exceptions.NotEnoughDataException;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.exceptions.TooBigDataException;
import com.baidu.brpc.protocol.AbstractProtocol;
import com.baidu.brpc.protocol.BaiduRpcErrno;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import com.baidu.brpc.protocol.RpcResponse;
import com.baidu.brpc.server.ServiceManager;
import com.baidu.brpc.utils.ProtobufUtils;
import com.baidu.brpc.utils.RpcMetaUtils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

/**
 * Notes on HULU PBRPC Protocol:
 * <ul>
 * <li> Header format is ["HULU"][<code>body_size</code>][<code>meta_size
 * </code>], 12 bytes in total
 * </li>
 * <li> {@code body_size} and {@code meta_size} are <b>NOT</b> in
 * <b>network</b> byte order (little endian)
 * </li>
 * <li> Use service->name() and method_index to identify the service and
 * method to call </li>
 * <li> {@code user_message_size} is set iff request/response has attachment
 * </li>
 * <li> The following fields of rpc are not supported yet:
 * <ul><li>chunk_info</li></ul>
 * </li>
 * </ul>
 */
public class HuluRpcProtocol extends AbstractProtocol {
    private static final Logger LOG = LoggerFactory.getLogger(HuluRpcProtocol.class);
    private static final byte[] MAGIC_HEAD = "HULU".getBytes();
    private static final int FIXED_LEN = 12;
    private static final HuluRpcProto.HuluRpcRequestMeta defaultRpcRequestMetaInstance =
            HuluRpcProto.HuluRpcRequestMeta.getDefaultInstance();
    private static final HuluRpcProto.HuluRpcResponseMeta defaultRpcResponseMetaInstance =
            HuluRpcProto.HuluRpcResponseMeta.getDefaultInstance();
    private static final CompressManager compressManager = CompressManager.getInstance();

    @Override
    public ByteBuf encodeRequest(Request request) throws Exception {
        HuluRpcEncodePacket requestPacket = new HuluRpcEncodePacket();
        HuluRpcProto.HuluRpcRequestMeta.Builder metaBuilder = HuluRpcProto.HuluRpcRequestMeta.newBuilder();
        metaBuilder.setCorrelationId(request.getLogId());
        metaBuilder.setLogId(request.getLogId());
        int compressType = request.getCompressType();
        metaBuilder.setCompressType(compressType);

        // service method
        RpcMetaUtils.RpcMetaInfo rpcMetaInfo = RpcMetaUtils.parseRpcMeta(request.getTargetMethod());
        metaBuilder.setServiceName(rpcMetaInfo.getServiceName());
        try {
            int methodIndex = Integer.valueOf(rpcMetaInfo.getMethodName());
            metaBuilder.setMethodIndex(methodIndex);
        } catch (NumberFormatException ex) {
            String errorMsg = "methodName must be integer when using hulu rpc, "
                    + "it is equal to proto method sequence from 0";
            LOG.warn(errorMsg);
            throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, errorMsg, ex);
        }

        if (request.getTraceId() != null) {
            metaBuilder.setTraceId(request.getTraceId());
        }
        if (request.getSpanId() != null) {
            metaBuilder.setSpanId(request.getSpanId());
        }
        if (request.getParentSpanId() != null) {
            metaBuilder.setSpanId(request.getParentSpanId());
        }
        if (request.getKvAttachment() != null) {
            for (Map.Entry<String, Object> kv : request.getKvAttachment().entrySet()) {
                metaBuilder.addExtFieldsBuilder().setKey(kv.getKey()).setValue((String) kv.getValue());
            }
        }

        // proto
        Object proto = request.getArgs()[0];
        Compress compress = compressManager.getCompress(compressType);
        ByteBuf protoBuf = compress.compressInput(proto, request.getRpcMethodInfo());
        requestPacket.setProto(protoBuf);

        // attachement
        if (request.getBinaryAttachment() != null
                && request.getBinaryAttachment().isReadable()) {
            requestPacket.setAttachment(request.getBinaryAttachment());
            metaBuilder.setUserMessageSize(protoBuf.readableBytes());
        }
        requestPacket.setRequestMeta(metaBuilder.build());
        return encode(requestPacket);
    }

    @Override
    public Response decodeResponse(Object packet, ChannelHandlerContext ctx) throws Exception {
        HuluRpcDecodePacket responsePacket = (HuluRpcDecodePacket) packet;
        ByteBuf metaBuf = responsePacket.getMetaBuf();
        ByteBuf protoAndAttachmentBuf = responsePacket.getProtoAndAttachmentBuf();
        ByteBuf protoBuf = null;
        try {
            RpcResponse rpcResponse = new RpcResponse();
            HuluRpcProto.HuluRpcResponseMeta responseMeta = (HuluRpcProto.HuluRpcResponseMeta) ProtobufUtils.parseFrom(
                    metaBuf, defaultRpcResponseMetaInstance);
            Long logId = responseMeta.getCorrelationId();
            rpcResponse.setLogId(logId);

            ChannelInfo channelInfo = ChannelInfo.getClientChannelInfo(ctx.channel());
            RpcFuture future = channelInfo.removeRpcFuture(rpcResponse.getLogId());
            if (future == null) {
                return rpcResponse;
            }
            rpcResponse.setRpcFuture(future);
            int compressType = responseMeta.getCompressType();
            rpcResponse.setCompressType(compressType);
            try {
                if (responseMeta != null && responseMeta.getErrorCode() == 0) {
                    Compress compress = compressManager.getCompress(compressType);
                    if (responseMeta.getUserMessageSize() > 0) {
                        protoBuf = protoAndAttachmentBuf.slice(
                                protoAndAttachmentBuf.readerIndex(),
                                responseMeta.getUserMessageSize());
                    } else {
                        protoBuf = protoAndAttachmentBuf;
                    }
                    Object responseProto = compress.uncompressOutput(protoBuf, future.getRpcMethodInfo());
                    rpcResponse.setResult(responseProto);

                    // attachment
                    if (responseMeta.getUserMessageSize() > 0) {
                        rpcResponse.setBinaryAttachment(protoAndAttachmentBuf);
                        responsePacket.setProtoAndAttachmentBuf(null);
                    }
                } else {
                    rpcResponse.setException(new RpcException(
                            RpcException.SERVICE_EXCEPTION, responseMeta.getErrorText()));
                }
            } catch (Exception ex) {
                // 解析失败直接抛异常
                throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, "decode response failed", ex);
            }
            return rpcResponse;
        } finally {
            if (responsePacket.getMetaBuf() != null) {
                responsePacket.getMetaBuf().release();
            }
            if (responsePacket.getProtoAndAttachmentBuf() != null) {
                responsePacket.getProtoAndAttachmentBuf().release();
            }
        }

    }

    @Override
    public Request decodeRequest(Object packet) throws Exception {
        Request request = this.createRequest();
        HuluRpcDecodePacket requestPacket = (HuluRpcDecodePacket) packet;
        ByteBuf metaBuf = requestPacket.getMetaBuf();
        ByteBuf protoAndAttachmentBuf = requestPacket.getProtoAndAttachmentBuf();
        ByteBuf protoBuf = null;
        try {
            HuluRpcProto.HuluRpcRequestMeta requestMeta = (HuluRpcProto.HuluRpcRequestMeta) ProtobufUtils.parseFrom(
                    metaBuf, defaultRpcRequestMetaInstance);
            request.setLogId(requestMeta.getCorrelationId());
            int compressType = requestMeta.getCompressType();
            request.setCompressType(compressType);

            // service info
            ServiceManager serviceManager = ServiceManager.getInstance();
            RpcMethodInfo rpcMethodInfo = serviceManager.getService(
                    requestMeta.getServiceName(), String.valueOf(requestMeta.getMethodIndex()));
            if (rpcMethodInfo == null) {
                String errorMsg = String.format("Fail to find service=%s, methodIndex=%s",
                        requestMeta.getServiceName(), requestMeta.getMethodIndex());
                request.setException(new RpcException(RpcException.SERVICE_EXCEPTION, errorMsg));
                return request;
            }
            if (requestMeta.hasTraceId()) {
                request.setTraceId(requestMeta.getTraceId());
            }
            if (requestMeta.hasSpanId()) {
                request.setSpanId(request.getSpanId());
            }
            if (requestMeta.hasParentSpanId()) {
                request.setParentSpanId(requestMeta.getParentSpanId());
            }
            if (requestMeta.getExtFieldsCount() > 0) {
                if (request.getKvAttachment() == null) {
                    request.setKvAttachment(new HashMap<String, Object>());
                }
                for (HuluRpcProto.HuluRpcRequestMetaExtField extField : requestMeta.getExtFieldsList()) {
                    request.getKvAttachment().put(extField.getKey(), extField.getValue());
                }
            }
            request.setServiceName(rpcMethodInfo.getServiceName());
            request.setMethodName(rpcMethodInfo.getMethodName());
            request.setRpcMethodInfo(rpcMethodInfo);
            request.setTargetMethod(rpcMethodInfo.getMethod());
            request.setTarget(rpcMethodInfo.getTarget());

            // proto body
            try {
                Compress compress = compressManager.getCompress(compressType);
                int userMessageSize = requestMeta.getUserMessageSize();
                if (userMessageSize > 0) {
                    protoBuf = protoAndAttachmentBuf.slice(
                            protoAndAttachmentBuf.readerIndex(),
                            userMessageSize);
                } else {
                    protoBuf = protoAndAttachmentBuf;
                }
                Object requestProto = compress.uncompressInput(protoBuf, rpcMethodInfo);
                request.setArgs(new Object[] {requestProto});
                // attachment
                if (userMessageSize > 0) {
                    request.setBinaryAttachment(protoAndAttachmentBuf);
                    protoAndAttachmentBuf = null;
                }
            } catch (Exception ex) {
                String errorMsg = String.format("decode failed, msg=%s", ex.getMessage());
                LOG.error(errorMsg);
                throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, errorMsg, ex);
            }
            return request;
        } finally {
            if (metaBuf != null) {
                metaBuf.release();
            }
            if (protoAndAttachmentBuf != null) {
                protoAndAttachmentBuf.release();
            }
        }
    }

    @Override
    public ByteBuf encodeResponse(Request request, Response response) throws Exception {
        HuluRpcEncodePacket responsePacket = new HuluRpcEncodePacket();
        HuluRpcProto.HuluRpcResponseMeta.Builder metaBuilder = HuluRpcProto.HuluRpcResponseMeta.newBuilder();
        metaBuilder.setCorrelationId(response.getLogId());
        int compressType = response.getCompressType();
        metaBuilder.setCompressType(compressType);

        if (response.getException() != null) {
            metaBuilder.setErrorCode(BaiduRpcErrno.Errno.EINTERNAL_VALUE);
            if (StringUtils.isNotBlank(response.getException().getMessage())) {
                metaBuilder.setErrorText(response.getException().getMessage());
            }
            responsePacket.setResponseMeta(metaBuilder.build());
        } else {
            metaBuilder.setErrorCode(0);
            Compress compress = compressManager.getCompress(compressType);
            ByteBuf responseProtoBuf = compress.compressOutput(
                    response.getResult(), response.getRpcMethodInfo());
            responsePacket.setProto(responseProtoBuf);

            // attachment
            if (response.getBinaryAttachment() != null) {
                responsePacket.setAttachment(response.getBinaryAttachment());
                metaBuilder.setUserMessageSize(responseProtoBuf.readableBytes());
            }
            responsePacket.setResponseMeta(metaBuilder.build());
        }

        return encode(responsePacket);
    }

    protected ByteBuf encode(HuluRpcEncodePacket packet) throws IOException {
        int metaSize;
        ByteBuf metaBuf;

        HuluRpcProto.HuluRpcRequestMeta requestMeta = packet.getRequestMeta();
        if (requestMeta != null) {
            // request
            byte[] metaBytes = requestMeta.toByteArray();
            metaSize = metaBytes.length;
            metaBuf = Unpooled.wrappedBuffer(metaBytes);
        } else {
            // response
            byte[] metaBytes = packet.getResponseMeta().toByteArray();
            metaSize = metaBytes.length;
            metaBuf = Unpooled.wrappedBuffer(metaBytes);
        }

        // fixed header buf
        ByteBuf headerBuf = Unpooled.buffer(FIXED_LEN);
        headerBuf.writeBytes(MAGIC_HEAD);
        int bodySize = metaSize;
        ByteBuf protoBuf = packet.getProto();
        if (protoBuf != null) {
            bodySize += protoBuf.readableBytes();
        }
        ByteBuf attachmentBuf = packet.getAttachment();
        if (attachmentBuf != null) {
            bodySize += attachmentBuf.readableBytes();
        }
        headerBuf.writeIntLE(bodySize);
        headerBuf.writeIntLE(metaSize);

        if (protoBuf != null && attachmentBuf != null) {
            return Unpooled.wrappedBuffer(headerBuf, metaBuf, protoBuf, attachmentBuf);
        } else if (protoBuf != null) {
            return Unpooled.wrappedBuffer(headerBuf, metaBuf, protoBuf);
        } else {
            return Unpooled.wrappedBuffer(headerBuf, metaBuf);
        }
    }

    @Override
    public HuluRpcDecodePacket decode(ChannelHandlerContext ctx, DynamicCompositeByteBuf in, boolean isDecodingRequest)
            throws BadSchemaException, TooBigDataException, NotEnoughDataException {
        if (in.readableBytes() < FIXED_LEN) {
            throw notEnoughDataException;
        }
        ByteBuf fixHeaderBuf = in.retainedSlice(FIXED_LEN);

        try {
            byte[] magic = new byte[4];
            fixHeaderBuf.readBytes(magic);
            if (!Arrays.equals(magic, MAGIC_HEAD)) {
                throw new BadSchemaException("not valid magic head for hulu");
            }
            int bodySize = fixHeaderBuf.readIntLE();
            int metaSize = fixHeaderBuf.readIntLE();
            // 512M
            if (bodySize > 512 * 1024 * 1024) {
                throw new TooBigDataException("to big body size:" + bodySize);
            }
            if (in.readableBytes() < FIXED_LEN + bodySize) {
                throw notEnoughDataException;
            }

            in.skipBytes(FIXED_LEN);
            HuluRpcDecodePacket packet = new HuluRpcDecodePacket();
            try {
                // meta
                ByteBuf metaBuf = in.readRetainedSlice(metaSize);
                packet.setMetaBuf(metaBuf);

                // body
                ByteBuf protoAndAttachmentBuf = in.readRetainedSlice(bodySize - metaSize);
                packet.setProtoAndAttachmentBuf(protoAndAttachmentBuf);

                return packet;
            } catch (Exception ex) {
                LOG.warn("decode failed, ex={}", ex.getMessage());
                throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, ex);
            }
        } finally {
            fixHeaderBuf.release();
        }
    }

    @Override
    public boolean isCoexistence() {
        return true;
    }

}
