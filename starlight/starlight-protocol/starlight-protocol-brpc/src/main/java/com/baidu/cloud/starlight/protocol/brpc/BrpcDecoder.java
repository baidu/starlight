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
 
package com.baidu.cloud.starlight.protocol.brpc;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.exception.CodecException;
import com.baidu.cloud.starlight.api.extension.ExtensionLoader;
import com.baidu.cloud.starlight.api.model.MsgBase;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.model.RpcResponse;
import com.baidu.cloud.starlight.api.model.Wrapper;
import com.baidu.cloud.starlight.api.serialization.serializer.Serializer;
import com.baidu.cloud.starlight.api.transport.buffer.DynamicCompositeByteBuf;
import com.baidu.cloud.starlight.api.protocol.Protocol;
import com.baidu.cloud.starlight.api.protocol.ProtocolDecoder;
import com.baidu.cloud.starlight.api.utils.ByteArrayUtils;
import com.baidu.cloud.starlight.api.utils.StringUtils;
import com.baidu.cloud.starlight.serialization.serializer.ProtobufSerializer;
import com.baidu.cloud.thirdparty.netty.buffer.ByteBuf;
import com.baidu.cloud.starlight.serialization.serializer.ProtoStuffSerializer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by liuruisen on 2020/2/6.
 */
public class BrpcDecoder implements ProtocolDecoder {

    @Override
    public MsgBase decode(DynamicCompositeByteBuf input) throws CodecException {

        // not enough data
        if (input.readableBytes() < BrpcProtocol.FIXED_LEN) { // not enough data
            throw new CodecException(CodecException.PROTOCOL_INSUFFICIENT_DATA_EXCEPTION,
                "Too little data to parse using Brpc"); // wait and retry
        }

        ByteBuf fixHeaderBuf = input.retainedSlice(BrpcProtocol.FIXED_LEN);
        ByteBuf metaBuf = null;
        ByteBuf dataBuf = null;
        try {
            byte[] magic = new byte[4];
            fixHeaderBuf.readBytes(magic);
            if (!Arrays.equals(magic, BrpcProtocol.MAGIC_HEAD)) { // not match
                throw new CodecException(CodecException.PROTOCOL_DECODE_NOTMATCH_EXCEPTION,
                    "Magic num dose not match Brpc");
            }

            // the whole body size = meta data size + body data size
            int bodySize = fixHeaderBuf.readInt();
            if (input.readableBytes() < BrpcProtocol.FIXED_LEN + bodySize) {
                throw new CodecException(CodecException.PROTOCOL_DECODE_NOTENOUGHDATA_EXCEPTION,
                    "Data not enough to parse using Brpc"); // wait and retry
            }
            // 512M
            if (bodySize > BrpcProtocol.MAX_BODY_SIZE) {
                throw new CodecException(CodecException.PROTOCOL_DECODE_EXCEPTION,
                    "Data size is bigger than max_body_size(512M), the size is " + bodySize); // failed
            }

            // meta data size
            int metaSize = fixHeaderBuf.readInt();

            input.skipBytes(BrpcProtocol.FIXED_LEN); // skip head

            metaBuf = input.readRetainedSlice(metaSize);

            // decode meta data in IO Thread
            MsgBase output = decodeMeta(metaBuf);

            // body data size
            int dataSize = bodySize - metaSize;
            if (dataSize > 0) {
                dataBuf = input.readRetainedSlice(dataSize);
                if (dataBuf == null || dataBuf.readableBytes() == 0) {
                    throw new CodecException(CodecException.PROTOCOL_DECODE_EXCEPTION,
                        "Brpc decode failed, brpc body data is null");
                }
                // data body: body data + binary attach data, remember to release
                byte[] dataBytes = new byte[dataSize]; // data
                dataBuf.readBytes(dataBytes);
                output.setBodyBytes(dataBytes);
            }
            // set protocol name
            output.setProtocolName(BrpcProtocol.PROTOCOL_NAME);

            return output;
        } finally {
            fixHeaderBuf.release();
            if (metaBuf != null) {
                metaBuf.release();
            }
            if (dataBuf != null) {
                dataBuf.release();
            }
        }
    }

    private MsgBase decodeMeta(ByteBuf metaBuf) throws CodecException {
        if (metaBuf == null || metaBuf.readableBytes() == 0) {
            throw new CodecException(CodecException.PROTOCOL_DECODE_EXCEPTION,
                "Brpc decode failed, brpc meta data is null");
        }
        MsgBase msgBase = null;
        try {
            final int brpcMetaLength = metaBuf.readableBytes();
            byte[] brpcMetaArray = new byte[brpcMetaLength];
            metaBuf.readBytes(brpcMetaArray, 0, brpcMetaLength);

            ProtoStuffSerializer serializer =
                (ProtoStuffSerializer) serializer(BrpcProtocol.SERIALIZER_TYPE_PROTOSTUFF);
            BrpcMeta rpcMeta =
                (BrpcMeta) serializer.deserialize(brpcMetaArray, BrpcMeta.class, BrpcProtocol.metaStrategyFlag(null));

            if (rpcMeta != null) {
                if (rpcMeta.getRequest() != null && !StringUtils.isEmpty(rpcMeta.getRequest().getServiceName())) {
                    msgBase = requestMeta(rpcMeta);
                }

                if (rpcMeta.getResponse() != null && rpcMeta.getResponse().getErrorCode() != null) {
                    msgBase = responseMeta(rpcMeta);
                }

                // attachment size 及其 binary attachment何种场景下使用？
                if (rpcMeta.getAttachmentSize() != null && rpcMeta.getAttachmentSize() > 0 && msgBase != null) {
                    if (msgBase.getAttachmentKv() == null) {
                        Map<String, Object> kvMap = new HashMap<>();
                        msgBase.setAttachmentKv(kvMap);
                    }
                    msgBase.getAttachmentKv().put(BrpcProtocol.BINARY_ATTACH_SIZE_KEY, rpcMeta.getAttachmentSize());
                }
            }
        } catch (Exception e) {
            throw new CodecException("Brpc decode metaBuf failed, deserialize meta error", e);
        }

        return msgBase;
    }

    private Request requestMeta(BrpcMeta rpcMeta) {
        Request request = new RpcRequest(rpcMeta.getCorrelationId());
        // msgBase common information
        request.setCompressType(rpcMeta.getCompressType());
        request.setHeartbeat(false); // not support heartbeat message
        // protocol
        request.setProtocolName(BrpcProtocol.PROTOCOL_NAME);

        // TODO Streaming模式尚未使用

        // Request information
        BrpcRequestMeta requestMeta = rpcMeta.getRequest();
        request.setServiceName(requestMeta.getServiceName());
        request.setMethodName(requestMeta.getMethodName());

        // attachmentKv
        Map<String, Object> kvMap = new HashMap<>();

        // auth data
        if (rpcMeta.getAuthenticationData() != null && rpcMeta.getAuthenticationData().length > 0) {
            kvMap.put(BrpcProtocol.AUTH_KEY, new String(rpcMeta.getAuthenticationData()));
        }

        // requestId
        kvMap.put(Constants.REQUEST_ID_KEY, requestMeta.getLogId());
        // trace
        kvMap.put(Constants.TRACE_ID_KEY, requestMeta.getTraceId());
        kvMap.put(Constants.SPAN_ID_KEY, requestMeta.getSpanId());
        kvMap.put(Constants.PARENT_SPAN_ID_KEY, requestMeta.getParentSpanId());
        // kv attach
        if (requestMeta.getExtFields() != null && requestMeta.getExtFields().size() > 0) {
            for (BrpcRequestMeta.BrpcRequestMetaExt extField : requestMeta.getExtFields()) {
                kvMap.put(extField.getKey(), extField.getValue());
            }
        }
        // 兼容java传输Object类型的attachKv场景，全量attachKv由starlight request ext与brpc request ext组成
        if (requestMeta.getStarlightRequestMeta() != null
            && requestMeta.getStarlightRequestMeta().getStarlightExtFields() != null) {
            kvMap.putAll(requestMeta.getStarlightRequestMeta().getStarlightExtFields());
        }
        request.setAttachmentKv(kvMap);

        return request;
    }

    private Response responseMeta(BrpcMeta rpcMeta) {
        Response response = new RpcResponse(rpcMeta.getCorrelationId());
        // msgBase common information
        response.setCompressType(rpcMeta.getCompressType());

        // Response information
        BrpcResponseMeta responseMeta = rpcMeta.getResponse();
        response.setStatus(CodeMapping.getStarlightMappingOfBrpcNo(responseMeta.getErrorCode()));
        response.setErrorMsg(responseMeta.getErrorText());

        // attachmentKv
        Map<String, Object> kvMap = new HashMap<>();
        kvMap.put(Constants.PROTOCOL_KEY, BrpcProtocol.PROTOCOL_NAME);
        response.setAttachmentKv(kvMap);
        // starlight response meta
        if (responseMeta.getStarlightResponseMeta() != null) {
            kvMap.putAll(responseMeta.getStarlightResponseMeta().getStarlightExtFields());
        }

        return response;
    }

    @Override
    public void decodeBody(MsgBase msgBase) throws CodecException {
        if (msgBase == null) {
            throw new CodecException(CodecException.BODY_DECODE_EXCEPTION, "Message is null to decode");
        }
        if (msgBase.getBodyBytes() == null || msgBase.getBodyBytes().length == 0) { // body is null, return
            return;
        }

        int attachSize = 0; // binary attach
        if (msgBase.getAttachmentKv() != null
            && msgBase.getAttachmentKv().get(BrpcProtocol.BINARY_ATTACH_SIZE_KEY) != null) {
            attachSize = (int) msgBase.getAttachmentKv().get(BrpcProtocol.BINARY_ATTACH_SIZE_KEY);
            if (attachSize > 0) {
                byte[] dataBytes =
                    ByteArrayUtils.subByte(msgBase.getBodyBytes(), 0, msgBase.getBodyBytes().length - attachSize);
                byte[] attach = ByteArrayUtils.subByte(msgBase.getBodyBytes(),
                    msgBase.getBodyBytes().length - attachSize, attachSize);
                msgBase.setBodyBytes(dataBytes);
                msgBase.getAttachmentKv().put(BrpcProtocol.BINARY_ATTACH_KEY, attach);
            }
        }

        if (msgBase instanceof Request) {
            decodeRequestBody((Request) msgBase);
        }
        if (msgBase instanceof Response) {
            decodeResponseBody((Response) msgBase);
        }
    }

    private void decodeRequestBody(Request request) {
        Class[] originParamTypes = request.getParamsTypes();

        // Compatible with various problems in the Java scene
        BrpcProtocol.wrapReqParams(request);

        Serializer serializer = serializer(BrpcProtocol.bodySerType(request));

        Object object = null;
        if (serializer instanceof ProtoStuffSerializer) {
            ProtoStuffSerializer stuffSerializer = (ProtoStuffSerializer) serializer;
            // compatible empty collection in response body
            Integer idStrategyFlag = BrpcProtocol.bodyStrategyFlag(BrpcProtocol.bodySerMode(request));
            object = stuffSerializer.deserialize(request.getBodyBytes(), request.getParamsTypes()[0], idStrategyFlag);
        }

        if (serializer instanceof ProtobufSerializer) {
            ProtobufSerializer protobufSerializer = (ProtobufSerializer) serializer;
            object = protobufSerializer.deserialize(request.getBodyBytes(), request.getParamsTypes()[0]);
        }
        request.setParams(new Object[] {object});

        // revert wrap
        if (object instanceof Wrapper) { // multi params or generic call
            request.setParams((Object[]) ((Wrapper) object).getObj());
        }

        // revert wrap
        request.setParamsTypes(originParamTypes);
    }

    private void decodeResponseBody(Response response) {
        Class originRetType = response.getReturnType();

        // Compatible with various problems in the Java scene
        BrpcProtocol.wrapRespResult(response);

        Serializer serializer = serializer(BrpcProtocol.bodySerType(response.getRequest()));

        Object result = null;
        if (serializer instanceof ProtoStuffSerializer) {
            ProtoStuffSerializer stuffSerializer = (ProtoStuffSerializer) serializer;
            // compatible empty collection in response body
            Integer idStrategyFlag = BrpcProtocol.bodyStrategyFlag(BrpcProtocol.bodySerMode(response.getRequest()));
            result = stuffSerializer.deserialize(response.getBodyBytes(), response.getReturnType(), idStrategyFlag);
        }

        if (serializer instanceof ProtobufSerializer) {
            ProtobufSerializer protobufSerializer = (ProtobufSerializer) serializer;
            result = protobufSerializer.deserialize(response.getBodyBytes(), response.getReturnType());
        }
        response.setResult(result);

        if (result instanceof Wrapper) {
            Wrapper wrapper = (Wrapper) result;
            response.setResult(wrapper.getObj());
        }

        // revert wrap
        response.setReturnType(originRetType);
    }

    private Serializer serializer(String serializerType) {
        BrpcProtocol brpcProtocol =
            (BrpcProtocol) ExtensionLoader.getInstance(Protocol.class).getExtension(BrpcProtocol.PROTOCOL_NAME);
        return brpcProtocol.getSerialize(serializerType);
    }
}
