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

package com.baidu.brpc.protocol.sofa;

import java.util.Arrays;

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
import com.baidu.brpc.protocol.Options;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import com.baidu.brpc.protocol.RpcRequest;
import com.baidu.brpc.protocol.RpcResponse;
import com.baidu.brpc.server.ServiceManager;
import com.baidu.brpc.utils.ProtobufUtils;
import com.baidu.brpc.utils.RpcMetaUtils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

/**
 * Notes on SOFA PBRPC Protocol:
 * <ul>
 * <li> Header format is ["SOFA"][<code>meta_size</code>]
 * [<code>body_size(64)</code>][<code>message_size(64)</code>],
 * 24 bytes in total
 * </li>
 * <li> {@code body_size} and {@code meta_size} are <b>NOT</b> in
 * <b>network</b> byte order (little endian)
 * </li>
 * <li> meta of request and response are same, distinguished by SofaRpcMeta#type</li>
 * <li> sofa-pbrpc does not conduct log_id. </li>
 * <li> sofa-pbrpc does not support attachment. </li>
 * </ul>
 */
public class SofaRpcProtocol extends AbstractProtocol {

    private static final Logger LOG = LoggerFactory.getLogger(SofaRpcProtocol.class);
    private final static byte[] MAGIC_HEAD = "SOFA".getBytes();
    private static final int FIXED_LEN = 24;
    private static final SofaRpcProto.SofaRpcMeta defaultRpcMetaInstance =
            SofaRpcProto.SofaRpcMeta.getDefaultInstance();
    private static final CompressManager compressManager = CompressManager.getInstance();

    @Override
    public SofaRpcDecodePacket decode(ChannelHandlerContext ctx, DynamicCompositeByteBuf in, boolean isDecodingRequest)
            throws BadSchemaException, TooBigDataException, NotEnoughDataException {
        if (in.readableBytes() < FIXED_LEN) {
            throw notEnoughDataException;
        }
        ByteBuf fixHeaderBuf = in.retainedSlice(FIXED_LEN);
        try {
            byte[] magic = new byte[4];
            fixHeaderBuf.readBytes(magic);
            if (!Arrays.equals(magic, MAGIC_HEAD)) {
                throw new BadSchemaException("not valid magic head for sofa");
            }
            int metaSize = fixHeaderBuf.readIntLE();
            int bodySize = (int) fixHeaderBuf.readLongLE();
            int msgSize = (int) fixHeaderBuf.readLongLE();

            if (msgSize != metaSize + bodySize) {
                throw new BadSchemaException("msgSize != metaSize + bodySize");
            }
            if (in.readableBytes() < FIXED_LEN + msgSize) {
                throw notEnoughDataException;
            }

            // 512M
            if (bodySize > 512 * 1024 * 1024) {
                throw new TooBigDataException("to big body size:" + bodySize);
            }

            in.skipBytes(FIXED_LEN);
            SofaRpcDecodePacket packet = new SofaRpcDecodePacket();
            try {
                // meta
                ByteBuf metaBuf = in.readRetainedSlice(metaSize);
                packet.setMetaBuf(metaBuf);

                // body
                ByteBuf protoBuf = in.readRetainedSlice(bodySize);
                packet.setProtoBuf(protoBuf);
                return packet;
            } catch (Exception ex) {
                LOG.debug("decode failed, ex={}", ex.getMessage());
                throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, ex);
            }
        } finally {
            fixHeaderBuf.release();
        }
    }

    @Override
    public ByteBuf encodeRequest(Request request) throws Exception {
        SofaRpcEncodePacket requestPacket = new SofaRpcEncodePacket();
        SofaRpcProto.SofaRpcMeta.Builder metaBuilder = SofaRpcProto.SofaRpcMeta.newBuilder();
        metaBuilder.setType(SofaRpcProto.SofaRpcMeta.Type.REQUEST);
        metaBuilder.setSequenceId(request.getLogId());
        int compressType = request.getCompressType();
        metaBuilder.setCompressType(getSofaCompressType(compressType));

        RpcMetaUtils.RpcMetaInfo rpcMetaInfo = RpcMetaUtils.parseRpcMeta(request.getTargetMethod());
        metaBuilder.setMethod(rpcMetaInfo.getServiceName() + "." + rpcMetaInfo.getMethodName());
        requestPacket.setRpcMeta(metaBuilder.build());

        Object proto = request.getArgs()[0];
        Compress compress = compressManager.getCompress(compressType);
        ByteBuf protoBuf = compress.compressInput(proto, request.getRpcMethodInfo());
        requestPacket.setProto(protoBuf);

        return encode(requestPacket);
    }

    @Override
    public RpcResponse decodeResponse(Object packet, ChannelHandlerContext ctx) throws Exception {
        SofaRpcDecodePacket responsePacket = (SofaRpcDecodePacket) packet;
        ByteBuf metaBuf = responsePacket.getMetaBuf();
        ByteBuf protoBuf = responsePacket.getProtoBuf();
        try {
            RpcResponse rpcResponse = new RpcResponse();
            SofaRpcProto.SofaRpcMeta responseMeta = (SofaRpcProto.SofaRpcMeta) ProtobufUtils.parseFrom(
                    metaBuf, defaultRpcMetaInstance);
            Long logId = responseMeta.getSequenceId();
            rpcResponse.setLogId(logId);

            ChannelInfo channelInfo = ChannelInfo.getClientChannelInfo(ctx.channel());
            RpcFuture future = channelInfo.removeRpcFuture(rpcResponse.getLogId());
            if (future == null) {
                return rpcResponse;
            }
            rpcResponse.setRpcFuture(future);
            int compressType = getStandardCompressType(responseMeta.getCompressType());
            rpcResponse.setCompressType(compressType);

            try {
                if (responseMeta != null && responseMeta.getErrorCode() == 0) {
                    Compress compress = compressManager.getCompress(compressType);
                    Object result = compress.uncompressOutput(
                            protoBuf, future.getRpcMethodInfo());
                    rpcResponse.setResult(result);
                } else {
                    rpcResponse.setException(new RpcException(
                            RpcException.SERVICE_EXCEPTION, responseMeta.getReason()));
                }
            } catch (Exception ex) {
                LOG.warn("decode response failed");
                throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, "decode response failed", ex);
            }
            return rpcResponse;
        } finally {
            if (metaBuf != null) {
                metaBuf.release();
            }
            if (protoBuf != null) {
                protoBuf.release();
            }
        }
    }

    @Override
    public Request decodeRequest(Object packet) throws Exception {
        Request request = this.createRequest();
        SofaRpcDecodePacket requestPacket = (SofaRpcDecodePacket) packet;
        ByteBuf metaBuf = requestPacket.getMetaBuf();
        ByteBuf protoBuf = requestPacket.getProtoBuf();
        try {
            SofaRpcProto.SofaRpcMeta requestMeta = (SofaRpcProto.SofaRpcMeta) ProtobufUtils.parseFrom(
                    metaBuf, defaultRpcMetaInstance);
            request.setLogId(requestMeta.getSequenceId());

            ServiceManager serviceManager = ServiceManager.getInstance();
            RpcMethodInfo rpcMethodInfo = serviceManager.getService(requestMeta.getMethod());
            if (rpcMethodInfo == null) {
                String errorMsg = String.format("Fail to find method=%s", requestMeta.getMethod());
                LOG.error(errorMsg);
                request.setException(new RpcException(RpcException.SERVICE_EXCEPTION, errorMsg));
                return request;
            }
            request.setServiceName(rpcMethodInfo.getServiceName());
            request.setMethodName(rpcMethodInfo.getMethodName());
            request.setRpcMethodInfo(rpcMethodInfo);
            request.setTargetMethod(rpcMethodInfo.getMethod());
            request.setTarget(rpcMethodInfo.getTarget());

            int compressType = getStandardCompressType(requestMeta.getCompressType());
            request.setCompressType(compressType);
            Compress compress = compressManager.getCompress(compressType);
            try {
                Object requestProto = compress.uncompressInput(
                        protoBuf, rpcMethodInfo);
                request.setArgs(new Object[] {requestProto});
            } catch (Exception ex) {
                String errorMsg = String.format("decode request failed, msg=%s", ex.getMessage());
                LOG.error(errorMsg);
                throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, errorMsg);
            }
            return request;
        } finally {
            if (metaBuf != null) {
                metaBuf.release();
            }
            if (protoBuf != null) {
                protoBuf.release();
            }
        }
    }

    @Override
    public ByteBuf encodeResponse(Request request, Response response) throws Exception {
        SofaRpcEncodePacket responsePacket = new SofaRpcEncodePacket();
        SofaRpcProto.SofaRpcMeta.Builder metaBuilder = SofaRpcProto.SofaRpcMeta.newBuilder();
        metaBuilder.setType(SofaRpcProto.SofaRpcMeta.Type.RESPONSE);
        metaBuilder.setSequenceId(response.getLogId());
        int compressType = response.getCompressType();
        metaBuilder.setCompressType(getSofaCompressType(compressType));

        if (response.getException() != null) {
            metaBuilder.setErrorCode(BaiduRpcErrno.Errno.ERESPONSE_VALUE);
            metaBuilder.setReason("invoke method failed");
            responsePacket.setRpcMeta(metaBuilder.build());
        } else {
            Object responseBodyMessage = response.getResult();
            Compress compress = compressManager.getCompress(compressType);
            ByteBuf responseProtoBuf = compress.compressOutput(responseBodyMessage, response.getRpcMethodInfo());
            responsePacket.setProto(responseProtoBuf);
            metaBuilder.setErrorCode(0);
            responsePacket.setRpcMeta(metaBuilder.build());
        }
        return encode(responsePacket);
    }

    @Override
    public boolean isCoexistence() {
        return true;
    }

    protected ByteBuf encode(SofaRpcEncodePacket packet) throws Exception {
        // meta buf
        byte[] metaBytes = packet.getRpcMeta().toByteArray();
        ByteBuf metaBuf = Unpooled.wrappedBuffer(metaBytes);
        int metaSize = metaBytes.length;

        // header buf
        ByteBuf headerBuf = Unpooled.buffer(FIXED_LEN);
        headerBuf.writeBytes(MAGIC_HEAD);
        headerBuf.writeIntLE(metaSize);
        ByteBuf protoBuf = packet.getProto();
        if (protoBuf != null) {
            int protoSize = protoBuf.readableBytes();
            headerBuf.writeLongLE(protoSize);
            headerBuf.writeLongLE(metaSize + protoSize);
            return Unpooled.wrappedBuffer(headerBuf, metaBuf, protoBuf);
        } else {
            headerBuf.writeLongLE(0);
            headerBuf.writeLongLE(metaSize);
            return Unpooled.wrappedBuffer(headerBuf, metaBuf);
        }
    }

    protected SofaRpcProto.SofaCompressType getSofaCompressType(int compressType) {
        SofaRpcProto.SofaCompressType sofaCompressType;
        switch (compressType) {
            case Options.CompressType.COMPRESS_TYPE_NONE_VALUE:
                sofaCompressType = SofaRpcProto.SofaCompressType.SOFA_COMPRESS_TYPE_NONE;
                break;
            case Options.CompressType.COMPRESS_TYPE_SNAPPY_VALUE:
                sofaCompressType = SofaRpcProto.SofaCompressType.SOFA_COMPRESS_TYPE_SNAPPY;
                break;
            case Options.CompressType.COMPRESS_TYPE_GZIP_VALUE:
                sofaCompressType = SofaRpcProto.SofaCompressType.SOFA_COMPRESS_TYPE_GZIP;
                break;
            case Options.CompressType.COMPRESS_TYPE_ZLIB_VALUE:
                sofaCompressType = SofaRpcProto.SofaCompressType.SOFA_COMPRESS_TYPE_ZLIB;
                break;
            case Options.CompressType.COMPRESS_TYPE_LZ4_VALUE:
                sofaCompressType = SofaRpcProto.SofaCompressType.SOFA_COMPRESS_TYPE_LZ4;
                break;
            default:
                throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, "not support compress type");
        }
        return sofaCompressType;
    }

    protected int getStandardCompressType(SofaRpcProto.SofaCompressType sofaCompressType) {
        int standardCompressType;
        switch (sofaCompressType.getNumber()) {
            case SofaRpcProto.SofaCompressType.SOFA_COMPRESS_TYPE_NONE_VALUE:
                standardCompressType = Options.CompressType.COMPRESS_TYPE_NONE_VALUE;
                break;
            case SofaRpcProto.SofaCompressType.SOFA_COMPRESS_TYPE_SNAPPY_VALUE:
                standardCompressType = Options.CompressType.COMPRESS_TYPE_SNAPPY_VALUE;
                break;
            case SofaRpcProto.SofaCompressType.SOFA_COMPRESS_TYPE_GZIP_VALUE:
                standardCompressType = Options.CompressType.COMPRESS_TYPE_GZIP_VALUE;
                break;
            case SofaRpcProto.SofaCompressType.SOFA_COMPRESS_TYPE_ZLIB_VALUE:
                standardCompressType = Options.CompressType.COMPRESS_TYPE_ZLIB_VALUE;
                break;
            case SofaRpcProto.SofaCompressType.SOFA_COMPRESS_TYPE_LZ4_VALUE:
                standardCompressType = Options.CompressType.COMPRESS_TYPE_LZ4_VALUE;
                break;
            default:
                throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, "not support compress type");
        }
        return standardCompressType;
    }

}
