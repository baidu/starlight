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
import com.baidu.cloud.starlight.api.exception.StarlightRpcException;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.model.Wrapper;
import com.baidu.cloud.starlight.api.utils.GenericUtil;
import com.baidu.cloud.starlight.api.utils.StringUtils;
import com.baidu.cloud.starlight.serialization.serializer.ProtobufSerializer;
import com.baidu.cloud.thirdparty.protostuff.runtime.DefaultIdStrategy;
import com.baidu.cloud.thirdparty.protostuff.runtime.IdStrategy;
import com.baidu.cloud.starlight.api.protocol.HeartbeatTrigger;
import com.baidu.cloud.starlight.api.protocol.Protocol;
import com.baidu.cloud.starlight.api.protocol.ProtocolDecoder;
import com.baidu.cloud.starlight.api.protocol.ProtocolEncoder;
import com.baidu.cloud.starlight.serialization.serializer.ProtoStuffSerializer;
import com.baidu.cloud.starlight.api.serialization.serializer.Serializer;
import com.google.protobuf.MessageLite;

import java.util.Collection;
import java.util.Map;

/**
 * Brpc Standard Protocol SPI name: brpc Created by liuruisen on 2020/2/6.
 */
public class BrpcProtocol implements Protocol {

    public static final String SERIALIZER_TYPE_PROTOSTUFF = "protostuff";

    public static final String SERIALIZER_TYPE_PROTOBUF = "protobuf";

    /**
     * BRPC auth data key
     */
    protected static final String AUTH_KEY = "auth_data";

    /**
     * Protocol name : brpc
     */
    public static final String PROTOCOL_NAME = "brpc";

    /**
     * Brpc binary attachment size key: Brpc binary attachment will be stored in attachmentKv
     */
    protected static final String BINARY_ATTACH_SIZE_KEY = "binary_attachment_size";

    /**
     * Brpc binary attachment key
     */
    protected static final String BINARY_ATTACH_KEY = "binary_attachment";

    /**
     * Fixed length
     */
    protected static final int FIXED_LEN = 12;
    protected static final byte[] MAGIC_HEAD = "PRPC".getBytes();

    /**
     * MAX body size 512M
     */
    protected static final Integer MAX_BODY_SIZE = 512 * 1024 * 1024;

    /**
     * stargate protostuff IdStrategy
     */
    protected static final IdStrategy IDSTRATEGY;

    private static final ProtocolEncoder ENCODER = new BrpcEncoder();

    private static final ProtocolDecoder DECODER = new BrpcDecoder();

    private static final Serializer PROTO_STUFF_SERIALIZER = new ProtoStuffSerializer();

    private static final Serializer PROTOBUF_SERIALIZER = new ProtobufSerializer();

    static {
        /**
         * Stargate will specify the following protostuff configuration when serializing and deserializing. But these
         * configuration is not suitable for Brpc serialize/deserialize and these configurations will form flags for
         * building IdStrategy, so we directly generate flags to construct IdStragety
         *
         * Stargate configuration: System.setProperty("protostuff.runtime.collection_schema_on_repeated_fields",
         * "true"); System.setProperty("protostuff.runtime.morph_collection_interfaces", "true");
         * System.setProperty("protostuff.runtime.morph_map_interfaces", "true");
         *
         */
        int flags = 0;
        flags |= IdStrategy.AUTO_LOAD_POLYMORPHIC_CLASSES; // enable by default

        IDSTRATEGY = new DefaultIdStrategy(flags);
    }

    @Override
    public ProtocolEncoder getEncoder() {
        return ENCODER;
    }

    @Override
    public ProtocolDecoder getDecoder() {
        return DECODER;
    }

    @Override
    public HeartbeatTrigger getHeartbeatTrigger() {
        return null; // not support
    }

    @Override
    public Serializer getSerialize() {
        // 默认选择protostuff
        return getSerialize(SERIALIZER_TYPE_PROTOSTUFF);
    }

    public Serializer getSerialize(String serializerType) {
        // 选择protobuf作为序列化反序列化器
        if (SERIALIZER_TYPE_PROTOBUF.equalsIgnoreCase(serializerType)) {
            return PROTOBUF_SERIALIZER;
        }
        // 其余一律选择protostuff
        return PROTO_STUFF_SERIALIZER;
    }

    /**
     * Get serializeStrategyFlag by strategyType
     *
     * @param strategyType
     * @return
     */
    public static Integer bodyStrategyFlag(String strategyType) {
        /**
         * Stargate will specify the following protostuff configuration when serializing and deserializing. But these
         * configuration is not suitable for Brpc serialize/deserialize and these configurations will form flags for
         * building IdStrategy, so we directly generate flags to construct IdStragety
         *
         * Stargate configuration: System.setProperty("protostuff.runtime.collection_schema_on_repeated_fields",
         * "true"); System.setProperty("protostuff.runtime.morph_collection_interfaces", "true");
         * System.setProperty("protostuff.runtime.morph_map_interfaces", "true");
         *
         */
        int flags = 0;
        flags |= IdStrategy.AUTO_LOAD_POLYMORPHIC_CLASSES; // enable by default

        if (StringUtils.isEmpty(strategyType)) {
            return flags;
        }

        if (strategyType.equals(Constants.PROTO2_STD_MODE)) {
            return flags;
        }

        if (strategyType.equals(Constants.PROTO2_JAVA_MODE)) {
            flags |= IdStrategy.PRESERVE_NULL_ELEMENTS;
            return flags;
        }

        return flags;
    }

    /**
     * Get serializeStrategyFlag by strategyType
     *
     * @param strategyType
     * @return
     */
    public static Integer metaStrategyFlag(String strategyType) {
        int flags = 0;
        flags |= IdStrategy.AUTO_LOAD_POLYMORPHIC_CLASSES; // enable by default
        return flags;
    }

    public static String bodySerMode(Request request) {
        // brpc协议交互时兼容emptylist
        String serializeMode = null;
        if (request.getServiceConfig() != null && request.getServiceConfig().getSerializeMode() != null) {
            return request.getServiceConfig().getSerializeMode();
        }
        // 适用response场景
        if (request.getAttachmentKv() != null) {
            Object serMode = request.getAttachmentKv().get(Constants.SERIALIZER_MODE_KEY);
            if (serMode instanceof String) {
                serializeMode = (String) serMode;
            }
        }
        return serializeMode;
    }

    /**
     * Support both protobuf-java and protostuff
     * 
     * @param request
     * @return
     */
    public static String bodySerType(Request request) {

        // 兼容非Starlight的客户端调用starlight场景，此时附加信息中无serializerType信息
        if (request.getParamsTypes() != null && request.getParamsTypes().length == 1
            && MessageLite.class.isAssignableFrom(request.getParamsTypes()[0])) {
            return SERIALIZER_TYPE_PROTOBUF;
        }

        // 使用于跨语言、跨框架交互场景
        if (request.getReturnType() != null && MessageLite.class.isAssignableFrom(request.getReturnType())) {
            return SERIALIZER_TYPE_PROTOBUF;
        }
        return SERIALIZER_TYPE_PROTOSTUFF;
    }

    /**
     * 兼容Brpc协议在落地时的各种兼容问题
     * 
     * @param request
     */
    public static void wrapReqParams(Request request) {
        // serializeMode is pb2-java
        String serializeMode = BrpcProtocol.bodySerMode(request);
        if (!StringUtils.isEmpty(serializeMode) && serializeMode.equalsIgnoreCase(Constants.PROTO2_JAVA_MODE)) {
            request.setParams(new Object[] {new Wrapper(request.getParams())});
            request.setParamsTypes(new Class[] {Wrapper.class});
            return;
        }

        // generic request
        if (GenericUtil.isGenericMsg(request) || GenericUtil.isGenericCall(request)) {
            request.setParams(new Object[] {new Wrapper(request.getParams())});
            request.setParamsTypes(new Class[] {Wrapper.class});
            return;
        }

        // multi params 兼容2020.0.1版本根据参数个数 > 1 判断是否需要wrap的场景
        if (request.getParamsTypes() != null && request.getParamsTypes().length > 1) {
            request.setParams(new Object[] {new Wrapper(request.getParams())});
            request.setParamsTypes(new Class[] {Wrapper.class});
            return;
        }
    }

    /**
     * 兼容Brpc协议在落地时的各种兼容问题
     *
     * @param response
     */
    public static void wrapRespResult(Response response) {
        // serializeMode is pb2-java
        String serializeMode = BrpcProtocol.bodySerMode(response.getRequest());
        if (!StringUtils.isEmpty(serializeMode) && serializeMode.equalsIgnoreCase(Constants.PROTO2_JAVA_MODE)) {
            response.setResult(new Wrapper(response.getResult()));
            response.setReturnType(Wrapper.class);
            return;
        }

        if (GenericUtil.isGenericMsg(response)) { // generic
            response.setResult(new Wrapper(response.getResult()));
            response.setReturnType(Wrapper.class);
            return;
        }
    }

    public static boolean checkRequest(Request request) throws StarlightRpcException {
        // serializeMode is proto2-java
        String serializeMode = BrpcProtocol.bodySerMode(request);

        if (serializeMode == null || // default = pb2-std
            serializeMode.equals(Constants.PROTO2_STD_MODE)) {
            if (request.getParamsTypes().length > 1 && !GenericUtil.isGenericCall(request)) {

                throw new StarlightRpcException(StarlightRpcException.BAD_REQUEST,
                    "Multi-parameter calls are not allowed when use brpc std. "
                        + "To avoid exception use pb2-java serializeMode or Modify parameter type.");
            }

            if (Collection.class.isAssignableFrom(request.getReturnType())
                || Map.class.isAssignableFrom(request.getReturnType())) {
                throw new StarlightRpcException(StarlightRpcException.BAD_REQUEST,
                    "Return type is Collection or Map are not allowed when use brpc std. "
                        + "To avoid exception use pb2-java serializeMode or Modify return type.");
            }
        }

        // protobuf message not support Multi-parameter
        if (request.getParamsTypes() != null && request.getParamsTypes().length > 1
            && MessageLite.class.isAssignableFrom(request.getParamsTypes()[0])) {
            throw new StarlightRpcException(StarlightRpcException.BAD_REQUEST,
                "Multi-parameter calls are not allowed when use brpc std(protobuf message). "
                    + "To avoid exception modify method parameter info: only 1 param.");
        }

        return true;
    }

}
