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
import com.baidu.cloud.starlight.api.protocol.Protocol;
import com.baidu.cloud.starlight.api.protocol.ProtocolEncoder;
import com.baidu.cloud.starlight.api.serialization.serializer.Serializer;
import com.baidu.cloud.starlight.api.utils.ByteArrayUtils;
import com.baidu.cloud.starlight.api.utils.StringUtils;
import com.baidu.cloud.starlight.serialization.serializer.ProtobufSerializer;
import com.baidu.cloud.thirdparty.netty.buffer.ByteBuf;
import com.baidu.cloud.thirdparty.netty.buffer.Unpooled;
import com.baidu.cloud.starlight.serialization.serializer.ProtoStuffSerializer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.baidu.cloud.starlight.protocol.brpc.BrpcProtocol.FIXED_LEN;

/**
 * Created by liuruisen on 2020/2/6.
 */
public class BrpcEncoder implements ProtocolEncoder {

    @Override
    public ByteBuf encode(MsgBase input) throws CodecException {
        if (input == null) {
            throw new CodecException(CodecException.PROTOCOL_ENCODE_EXCEPTION, "MsgBase is null, cannot encode");
        }

        if (input instanceof Request) {
            return encodeRequest((Request) input);
        }

        if (input instanceof Response) {
            return encodeResponse((Response) input);
        }

        throw new CodecException(CodecException.PROTOCOL_ENCODE_EXCEPTION,
            "MsgBase type is illegal {" + input.getClass().getName() + "}, cannot encode");
    }

    private ByteBuf encodeRequest(Request request) {
        try {
            // request body: meta
            // BaiduRpcProto.RpcRequestMeta.Builder requestMeta = BaiduRpcProto.RpcRequestMeta.newBuilder();
            BrpcRequestMeta requestMeta = new BrpcRequestMeta();
            requestMeta.setLogId(request.getId());
            // serviceId(cross-language) or interfaceName(only java)
            requestMeta.setServiceName(request.getServiceName());
            requestMeta.setMethodName(request.getMethodName());

            // requestMeta
            if (request.getAttachmentKv() != null) {
                Map<String, Object> requestAttachKv = new HashMap<>(request.getAttachmentKv());

                // auth key and binary attach size key will be set to RpcMeta, remove to prevent repeated addition
                requestAttachKv.remove(BrpcProtocol.AUTH_KEY);
                requestAttachKv.remove(BrpcProtocol.BINARY_ATTACH_SIZE_KEY);
                requestAttachKv.remove(BrpcProtocol.BINARY_ATTACH_KEY);

                // trace id, span id, parent span id
                Object traceId = requestAttachKv.remove(Constants.TRACE_ID_KEY);
                if (traceId instanceof Long) {
                    requestMeta.setTraceId((Long) traceId);
                }

                Object spanId = requestAttachKv.remove(Constants.SPAN_ID_KEY);
                if (spanId instanceof Long) {
                    requestMeta.setSpanId((Long) spanId);
                }

                Object parentSpanId = requestAttachKv.remove(Constants.PARENT_SPAN_ID_KEY);
                if (parentSpanId instanceof Long) {
                    requestMeta.setParentSpanId((Long) parentSpanId);
                }

                // requestMeta ext map
                Map<String, Object> requestAttachKv2 = new HashMap<>(requestAttachKv);

                List<BrpcRequestMeta.BrpcRequestMetaExt> metaExtList = new LinkedList<>();
                for (Map.Entry<String, Object> kv : requestAttachKv.entrySet()) {
                    if (kv.getKey() != null && kv.getValue() != null && kv.getValue() instanceof String) {
                        BrpcRequestMeta.BrpcRequestMetaExt metaExt = new BrpcRequestMeta.BrpcRequestMetaExt();
                        metaExt.setKey(kv.getKey());
                        metaExt.setValue((String) kv.getValue());
                        metaExtList.add(metaExt);

                        requestAttachKv2.remove(kv.getKey()); // remove to prevent repeated addition
                    }
                }
                requestMeta.setExtFields(metaExtList);

                // starlight request meta
                if (requestAttachKv2.size() > 0) {
                    StarlightRequestMeta starlightRequestMeta = new StarlightRequestMeta();
                    starlightRequestMeta.setStarlightExtFields(requestAttachKv2);

                    requestMeta.setStarlightRequestMeta(starlightRequestMeta);
                }
            }

            // rpc meta
            BrpcMeta meta = new BrpcMeta();
            meta.setCorrelationId(request.getId());
            meta.setCompressType(request.getCompressType());
            meta.setRequest(requestMeta);

            if (request.getAttachmentKv() != null) {
                // auth data
                Object authObject = request.getAttachmentKv().get(BrpcProtocol.AUTH_KEY);
                if (authObject instanceof String) {
                    String authData = (String) authObject;
                    meta.setAuthenticationData(authData.getBytes());
                }

                // binaryAttachment
                Object binarySizeObject = request.getAttachmentKv().get(BrpcProtocol.BINARY_ATTACH_SIZE_KEY);
                if (binarySizeObject instanceof Integer) {
                    Integer binaryAttachmentSize = (Integer) binarySizeObject;
                    meta.setAttachmentSize(binaryAttachmentSize);
                }
            }

            Integer idStrategyFlag = BrpcProtocol.metaStrategyFlag(BrpcProtocol.bodySerMode(request));

            // request body: data
            // data had been serialize and compress in Work Thread: data + binary attach
            ProtoStuffSerializer serializer =
                (ProtoStuffSerializer) serializer(BrpcProtocol.SERIALIZER_TYPE_PROTOSTUFF);
            return protocolEncode(serializer.serialize(meta, BrpcMeta.class, idStrategyFlag), request.getBodyBytes());
        } catch (Exception e) {
            throw new CodecException(CodecException.PROTOCOL_ENCODE_EXCEPTION, "Use brpc encode request failed!", e);
        }
    }

    private ByteBuf encodeResponse(Response response) {
        // rpc meta
        BrpcMeta meta = new BrpcMeta();
        meta.setCorrelationId(response.getId());
        meta.setCompressType(response.getCompressType());

        // response meta
        BrpcResponseMeta responseMeta = new BrpcResponseMeta();
        responseMeta.setErrorCode(CodeMapping.getBrpcMappingOfStarlightNo(response.getStatus()));
        if (response.getErrorMsg() != null) {
            responseMeta.setErrorText(response.getErrorMsg());
        }
        // starlight response meta contains
        responseMeta.setStarlightResponseMeta(generateStarlightRespMeta(response));

        meta.setResponse(responseMeta);

        if (response.getAttachmentKv() != null) {
            // binaryAttachment
            Object binarySizeObject = response.getAttachmentKv().get(BrpcProtocol.BINARY_ATTACH_SIZE_KEY);
            if (binarySizeObject instanceof Integer) {
                Integer binaryAttachmentSize = (Integer) binarySizeObject;
                meta.setAttachmentSize(binaryAttachmentSize);
            }
        }

        Integer idStrategyFlag = BrpcProtocol.metaStrategyFlag(BrpcProtocol.bodySerMode(response.getRequest()));

        ProtoStuffSerializer serializer = (ProtoStuffSerializer) serializer(BrpcProtocol.SERIALIZER_TYPE_PROTOSTUFF);
        // data had been serialize and compress in Work Thread: data + binary attach
        return protocolEncode(serializer.serialize(meta, BrpcMeta.class, idStrategyFlag), response.getBodyBytes());
    }

    /**
     * Starlight brpc特有的Response meta信息，比如用于日志记录的server端执行耗时情况
     * 
     * @param response
     * @return
     */
    private StarlightResponseMeta generateStarlightRespMeta(Response response) {
        addAdditionalRespKv(response);
        StarlightResponseMeta starlightResponseMeta = new StarlightResponseMeta();
        starlightResponseMeta.setStarlightExtFields(response.getAttachmentKv());
        return starlightResponseMeta;
    }

    private ByteBuf protocolEncode(byte[] metaBytes, byte[] dataBytes) {
        // header buf
        ByteBuf headerBuf = Unpooled.buffer(FIXED_LEN);
        // magic num
        headerBuf.writeBytes(BrpcProtocol.MAGIC_HEAD);
        int bodySize = metaBytes.length;
        if (dataBytes != null && dataBytes.length > 0) {
            bodySize = bodySize + dataBytes.length;
        }
        // body size
        headerBuf.writeInt(bodySize);
        // meta size
        headerBuf.writeInt(metaBytes.length);

        // body buf
        ByteBuf bodyBuf = null;
        // body
        if (dataBytes != null && dataBytes.length > 0) {
            bodyBuf = Unpooled.wrappedBuffer(metaBytes, dataBytes);
        } else {
            bodyBuf = Unpooled.wrappedBuffer(metaBytes);
        }

        return Unpooled.wrappedBuffer(headerBuf, bodyBuf);
    }

    @Override
    public void encodeBody(MsgBase msgBase) throws CodecException {
        if (msgBase == null) {
            throw new CodecException(CodecException.BODY_ENCODE_EXCEPTION, "MsgBase is null, cannot encode");
        }

        if (msgBase instanceof Request) {
            encodeRequestBody((Request) msgBase);
        }

        if (msgBase instanceof Response) {
            encodeResponseBody((Response) msgBase);
        }

        // support brpc attachment bytes
        if (msgBase.getAttachmentKv() != null
            && msgBase.getAttachmentKv().get(BrpcProtocol.BINARY_ATTACH_KEY) != null) {
            byte[] attachBinary = (byte[]) msgBase.getAttachmentKv().get(BrpcProtocol.BINARY_ATTACH_KEY);
            msgBase.getAttachmentKv().put(BrpcProtocol.BINARY_ATTACH_SIZE_KEY, attachBinary.length);

            byte[] bodyBytes = ByteArrayUtils.byteMerger(msgBase.getBodyBytes(), attachBinary);
            msgBase.setBodyBytes(bodyBytes);
        }
    }

    private void encodeRequestBody(Request request) throws CodecException {
        // add serialize strategy to attachment kv
        if (request.getServiceConfig() != null && !StringUtils.isEmpty(request.getServiceConfig().getSerializeMode())) {
            request.getAttachmentKv().put(Constants.SERIALIZER_MODE_KEY, request.getServiceConfig().getSerializeMode());
        }

        if (request.getParams() == null || request.getParams().length == 0) { // no body data
            return;
        }

        Class[] originParamTypes = request.getParamsTypes();
        Object[] originParams = request.getParams();

        // Compatible with various problems in the Java scene
        BrpcProtocol.wrapReqParams(request);

        Serializer serializer = serializer(BrpcProtocol.bodySerType(request));
        byte[] bodyBytes = null;
        if (serializer instanceof ProtoStuffSerializer) {
            ProtoStuffSerializer protoStuffSer = (ProtoStuffSerializer) serializer;
            Integer idStrategyFlag = BrpcProtocol.bodyStrategyFlag(BrpcProtocol.bodySerMode(request));
            bodyBytes = protoStuffSer.serialize(request.getParams()[0], request.getParamsTypes()[0], idStrategyFlag);
        }

        if (serializer instanceof ProtobufSerializer) {
            bodyBytes = serializer.serialize(request.getParams()[0], request.getParamsTypes()[0]);
        }

        request.setBodyBytes(bodyBytes);

        // revert wrap
        request.setParams(originParams);
        request.setParamsTypes(originParamTypes);
    }

    private void encodeResponseBody(Response response) throws CodecException {
        // no body data, the scene of the call error has been handled in #encodeResponse
        if (response.getResult() == null) {
            return;
        }

        Object originRet = response.getResult();
        Class originRetType = response.getReturnType();

        // Compatible with various problems in the Java scene
        BrpcProtocol.wrapRespResult(response);

        Serializer serializer = serializer(BrpcProtocol.bodySerType(response.getRequest()));
        byte[] bodyBytes = null;
        if (serializer instanceof ProtoStuffSerializer) {
            Integer idStrategyFlag = BrpcProtocol.bodyStrategyFlag(BrpcProtocol.bodySerMode(response.getRequest()));
            ProtoStuffSerializer stuffSerializer = (ProtoStuffSerializer) serializer;
            bodyBytes = stuffSerializer.serialize(response.getResult(), response.getReturnType(), idStrategyFlag);
        }

        if (serializer instanceof ProtobufSerializer) {
            ProtobufSerializer protobufSerializer = (ProtobufSerializer) serializer;
            bodyBytes = protobufSerializer.serialize(response.getResult(), response.getReturnType());
        }

        response.setBodyBytes(bodyBytes);

        response.setResult(originRet);
        response.setReturnType(originRetType);
    }

    private Serializer serializer(String serializerType) {
        BrpcProtocol brpcProtocol =
            (BrpcProtocol) ExtensionLoader.getInstance(Protocol.class).getExtension(BrpcProtocol.PROTOCOL_NAME);
        return brpcProtocol.getSerialize(serializerType);
    }
}
