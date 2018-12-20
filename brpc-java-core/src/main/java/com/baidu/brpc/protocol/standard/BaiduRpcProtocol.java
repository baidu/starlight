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

package com.baidu.brpc.protocol.standard;

import java.util.Arrays;

import com.baidu.brpc.exceptions.BadSchemaException;
import com.baidu.brpc.exceptions.NotEnoughDataException;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.exceptions.TooBigDataException;
import com.baidu.brpc.protocol.AbstractProtocol;
import com.baidu.brpc.protocol.BaiduRpcErrno;
import com.baidu.brpc.protocol.RpcRequest;
import com.baidu.brpc.protocol.RpcResponse;
import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.buffer.DynamicCompositeByteBuf;
import com.baidu.brpc.utils.ProtobufUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.brpc.ChannelInfo;
import com.baidu.brpc.client.RpcFuture;
import com.baidu.brpc.compress.Compress;
import com.baidu.brpc.compress.CompressManager;
import com.baidu.brpc.server.ServiceManager;
import io.netty.channel.ChannelHandlerContext;

/**
 * Notes on Baidu RPC Protocol:
 * <ul>
 * <li> Header format is ["PRPC"][<code>body_size</code>][<code>meta_size
 * </code>], 12 bytes in total
 * </li>
 * <li> {@code body_size} and {@code meta_size} are in <b>network</b> byte
 * order (big endian)
 * </li>
 * <li> Use service->full_name() and method_name to identify the service and
 * method to call </li>
 * <li> {@code attachment_size} is set iff request/response has attachment </li>
 * <li> The following fields of rpc are not supported yet:
 * <ul>
 * <li>chunk_info</li>
 * </ul>
 * </li>
 * </ul>
 * Created by huwenwei on 2017/9/22.
 */
public class BaiduRpcProtocol extends AbstractProtocol {

    private static final Logger LOG = LoggerFactory.getLogger(BaiduRpcProtocol.class);
    private static final byte[] MAGIC_HEAD = "PRPC".getBytes();
    private static final int FIXED_LEN = 12;
    private static final BaiduRpcProto.RpcMeta defaultRpcMetaInstance = BaiduRpcProto.RpcMeta.getDefaultInstance();
    private static final NotEnoughDataException notEnoughDataException =
            new NotEnoughDataException("not enough data");
    private static final CompressManager compressManager = CompressManager.getInstance();
    private static final ServiceManager serviceManager = ServiceManager.getInstance();

    @Override
    public ByteBuf encodeRequest(RpcRequest rpcRequest) throws Exception {
        BaiduRpcEncodePacket packet = new BaiduRpcEncodePacket();
        BaiduRpcProto.RpcMeta.Builder metaBuilder = BaiduRpcProto.RpcMeta.newBuilder();
        metaBuilder.setCorrelationId(rpcRequest.getLogId());
        int compressType = rpcRequest.getCompressType();
        metaBuilder.setCompressType(rpcRequest.getCompressType());

        BaiduRpcProto.RpcRequestMeta.Builder requestMeta = BaiduRpcProto.RpcRequestMeta.newBuilder();
        requestMeta.setLogId(rpcRequest.getLogId());
        requestMeta.setServiceName(rpcRequest.getServiceName());
        requestMeta.setMethodName(rpcRequest.getMethodName());
        metaBuilder.setRequest(requestMeta.build());

        // proto
        Compress compress = compressManager.getCompress(compressType);
        ByteBuf protoBuf = compress.compressInput(rpcRequest.getArgs()[0], rpcRequest.getRpcMethodInfo());
        packet.setProto(protoBuf);

        // attachment
        if (rpcRequest.getBinaryAttachment() != null
                && rpcRequest.getBinaryAttachment().isReadable()) {
            packet.setAttachment(rpcRequest.getBinaryAttachment());
            metaBuilder.setAttachmentSize(rpcRequest.getBinaryAttachment().readableBytes());
        }
        packet.setRpcMeta(metaBuilder.build());

        ByteBuf requestBuf = encode(packet);
        return requestBuf;
    }

    public RpcResponse decodeResponse(Object packet, ChannelHandlerContext ctx) throws Exception {
        BaiduRpcDecodePacket responsePacket = (BaiduRpcDecodePacket) packet;
        ByteBuf metaBuf = responsePacket.getMetaBuf();
        ByteBuf protoAndAttachmentBuf = responsePacket.getProtoAndAttachmentBuf();
        try {
            BaiduRpcProto.RpcMeta rpcMeta = (BaiduRpcProto.RpcMeta) ProtobufUtils.parseFrom(
                    metaBuf, defaultRpcMetaInstance);

            RpcResponse rpcResponse = new RpcResponse();
            long logId = rpcMeta.getCorrelationId();
            rpcResponse.setLogId(logId);

            ChannelInfo channelInfo = ChannelInfo.getClientChannelInfo(ctx.channel());
            RpcFuture future = channelInfo.removeRpcFuture(rpcResponse.getLogId());
            if (future == null) {
                return rpcResponse;
            }
            rpcResponse.setRpcFuture(future);
            BaiduRpcProto.RpcResponseMeta responseMeta = rpcMeta.getResponse();
            try {
                if (responseMeta != null && responseMeta.getErrorCode() == 0) {
                    Compress compress = compressManager.getCompress(rpcMeta.getCompressType());
                    int protoSize = protoAndAttachmentBuf.readableBytes()
                            - rpcMeta.getAttachmentSize();
                    // proto body
                    ByteBuf protoBuf = protoAndAttachmentBuf.readSlice(protoSize);
                    Object proto = compress.uncompressOutput(protoBuf, future.getRpcMethodInfo());
                    rpcResponse.setResult(proto);

                    // attachment
                    if (rpcMeta.getAttachmentSize() > 0) {
                        rpcResponse.setBinaryAttachment(protoAndAttachmentBuf);
                        protoAndAttachmentBuf = null;
                    }
                } else {
                    rpcResponse.setException(new RpcException(
                            RpcException.SERVICE_EXCEPTION, responseMeta.getErrorText()));
                }
            } catch (Exception ex) {
                // 解析失败直接抛异常
                throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, "decode response failed");
            }
            return rpcResponse;
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
    public BaiduRpcDecodePacket decode(DynamicCompositeByteBuf in)
            throws BadSchemaException, TooBigDataException, NotEnoughDataException {
        if (in.readableBytes() < FIXED_LEN) {
            throw notEnoughDataException;
        }
        ByteBuf fixHeaderBuf = in.retainedSlice(FIXED_LEN);
        try {
            byte[] magic = new byte[4];
            fixHeaderBuf.readBytes(magic);
            if (!Arrays.equals(magic, MAGIC_HEAD)) {
                throw new BadSchemaException("not valid magic head for brpc");
            }

            int bodySize = fixHeaderBuf.readInt();
            if (in.readableBytes() < FIXED_LEN + bodySize) {
                throw notEnoughDataException;
            }
            // 512M
            if (bodySize > 512 * 1024 * 1024) {
                throw new TooBigDataException("to big body size:" + bodySize);
            }

            int metaSize = fixHeaderBuf.readInt();
            in.skipBytes(FIXED_LEN);
            BaiduRpcDecodePacket packet = new BaiduRpcDecodePacket();
            try {
                // meta
                ByteBuf metaBuf = in.readRetainedSlice(metaSize);
                packet.setMetaBuf(metaBuf);

                // proto and attachment
                ByteBuf protoAndAttachmentBuf = in.readRetainedSlice(bodySize - metaSize);
                packet.setProtoAndAttachmentBuf(protoAndAttachmentBuf);

                return packet;
            } catch (Exception ex) {
                LOG.warn("decode failed:", ex);
                throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, ex);
            }
        } finally {
            fixHeaderBuf.release();
        }
    }

    @Override
    public void decodeRequest(Object packet, RpcRequest rpcRequest) throws Exception {
        BaiduRpcDecodePacket requestPacket = (BaiduRpcDecodePacket) packet;
        ByteBuf metaBuf = requestPacket.getMetaBuf();
        ByteBuf protoAndAttachmentBuf = requestPacket.getProtoAndAttachmentBuf();
        BaiduRpcProto.RpcMeta rpcMeta;
        try {
            rpcMeta = (BaiduRpcProto.RpcMeta) ProtobufUtils.parseFrom(
                    metaBuf, defaultRpcMetaInstance);
            BaiduRpcProto.RpcRequestMeta requestMeta = rpcMeta.getRequest();
            rpcRequest.setLogId(rpcMeta.getCorrelationId());
            int compressType = rpcMeta.getCompressType();
            rpcRequest.setCompressType(compressType);

            RpcMethodInfo rpcMethodInfo = serviceManager.getService(
                    requestMeta.getServiceName(), requestMeta.getMethodName());
            if (rpcMethodInfo == null) {
                String errorMsg = String.format("Fail to find service=%s, method=%s",
                        requestMeta.getServiceName(), requestMeta.getMethodName());
                LOG.error(errorMsg);
                rpcRequest.setException(new RpcException(RpcException.SERVICE_EXCEPTION, errorMsg));
                return;
            }
            rpcRequest.setRpcMethodInfo(rpcMethodInfo);
            rpcRequest.setTargetMethod(rpcMethodInfo.getMethod());
            rpcRequest.setTarget(rpcMethodInfo.getTarget());

            // proto body
            Compress compress = compressManager.getCompress(compressType);
            int protoSize = protoAndAttachmentBuf.readableBytes() - rpcMeta.getAttachmentSize();
            ByteBuf protoBuf = protoAndAttachmentBuf.readSlice(protoSize);
            Object proto = compress.uncompressInput(protoBuf, rpcMethodInfo);
            rpcRequest.setArgs(new Object[] {proto});

            // attachment
            if (rpcMeta.getAttachmentSize() > 0) {
                rpcRequest.setBinaryAttachment(protoAndAttachmentBuf);
                protoAndAttachmentBuf = null;
            }
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
    public ByteBuf encodeResponse(RpcResponse rpcResponse) throws Exception {
        BaiduRpcEncodePacket responsePacket = new BaiduRpcEncodePacket();
        BaiduRpcProto.RpcMeta.Builder metaBuilder = BaiduRpcProto.RpcMeta.newBuilder();
        metaBuilder.setCorrelationId(rpcResponse.getLogId());
        int compressType = rpcResponse.getCompressType();
        metaBuilder.setCompressType(compressType);
        BaiduRpcProto.RpcResponseMeta.Builder responseMetaBuilder = BaiduRpcProto.RpcResponseMeta.newBuilder();

        if (rpcResponse.getException() != null) {
            responseMetaBuilder.setErrorCode(BaiduRpcErrno.Errno.EINTERNAL_VALUE);
            responseMetaBuilder.setErrorText(rpcResponse.getException().getMessage());
            metaBuilder.setResponse(responseMetaBuilder.build());
            responsePacket.setRpcMeta(metaBuilder.build());
        } else {
            responseMetaBuilder.setErrorCode(0);
            metaBuilder.setResponse(responseMetaBuilder.build());

            Object responseProto = rpcResponse.getResult();
            Compress compress = compressManager.getCompress(compressType);
            ByteBuf protoBuf = compress.compressOutput(responseProto, rpcResponse.getRpcMethodInfo());
            responsePacket.setProto(protoBuf);

            // attachment
            if (rpcResponse.getBinaryAttachment() != null) {
                responsePacket.setAttachment(rpcResponse.getBinaryAttachment());
                metaBuilder.setAttachmentSize(rpcResponse.getBinaryAttachment().readableBytes());
            }
            responsePacket.setRpcMeta(metaBuilder.build());
        }

        ByteBuf resBuf = encode(responsePacket);
        return resBuf;
    }

    protected ByteBuf encode(BaiduRpcEncodePacket packet) throws Exception {
        byte[] metaBytes = packet.getRpcMeta().toByteArray();
        ByteBuf metaBuf = Unpooled.wrappedBuffer(metaBytes);
        int metaSize = metaBytes.length;

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
        headerBuf.writeInt(bodySize);
        headerBuf.writeInt(metaSize);
        if (protoBuf != null && attachmentBuf != null) {
            return Unpooled.wrappedBuffer(headerBuf, metaBuf, protoBuf, attachmentBuf);
        } else if (protoBuf != null) {
            return Unpooled.wrappedBuffer(headerBuf, metaBuf, protoBuf);
        } else {
            return Unpooled.wrappedBuffer(headerBuf, metaBuf);
        }
    }

}
