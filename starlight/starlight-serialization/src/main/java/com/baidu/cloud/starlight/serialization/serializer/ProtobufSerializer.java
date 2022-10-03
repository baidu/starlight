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
 
package com.baidu.cloud.starlight.serialization.serializer;

import com.baidu.cloud.starlight.api.exception.CodecException;
import com.baidu.cloud.starlight.api.serialization.serializer.Serializer;
import com.baidu.cloud.starlight.serialization.utils.SerializationUtils;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * 原生protobuf-java的序列化和反序列化类 使用brpc协议时支持业务使用.proto文件定义类对象
 */
public class ProtobufSerializer implements Serializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProtobufSerializer.class);

    @Override
    public byte[] serialize(Object inputObj, Type type) throws CodecException {
        if (inputObj == null) {
            throw new CodecException(CodecException.SERIALIZE_EXCEPTION,
                "The message used to protobuf serializing is null");
        }
        if (inputObj.getClass() != type || !isSupported(inputObj.getClass())) {
            throw new CodecException(CodecException.SERIALIZE_EXCEPTION,
                "The type used to protobuf serializing is illegal");
        }

        try {
            return ((Message) inputObj).toByteArray();
        } catch (Throwable e) {
            CodecException codecException = SerializationUtils.convertToCodecException(e,
                CodecException.SERIALIZE_EXCEPTION, "Protobuf serialize error");
            LOGGER.error("Protobuf deserialize error, ", codecException);
            throw codecException;
        }
    }

    @Override
    public Object deserialize(byte[] bytes, Type type) throws CodecException {
        if (bytes == null || bytes.length == 0) {
            throw new CodecException(CodecException.DESERIALIZE_EXCEPTION,
                "The bytes used to protobuf deserializing are empty");
        }

        if (!(type instanceof Class)) {
            throw new CodecException(CodecException.SERIALIZE_EXCEPTION,
                "The type to protobuf deserializing is not Class");
        }

        Class clazz = (Class) type;
        if (!isSupported(clazz)) {
            throw new CodecException(CodecException.DESERIALIZE_EXCEPTION,
                "The type used to protobuf deserializing are illegal");
        }

        try {
            Method outputParseFromMethod = clazz.getMethod("parseFrom", byte[].class);
            return outputParseFromMethod.invoke(type, bytes);
        } catch (Throwable e) {
            CodecException codecException = SerializationUtils.convertToCodecException(e,
                CodecException.DESERIALIZE_EXCEPTION, "Protobuf deserialize error");
            LOGGER.error("Protobuf deserialize error, ", codecException);
            throw codecException;
        }
    }

    private boolean isSupported(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }

        if (MessageLite.class.isAssignableFrom(clazz)) {
            return true;
        }
        return false;
    }
}