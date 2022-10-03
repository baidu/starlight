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

import com.baidu.cloud.thirdparty.dyuprotostuff.LinkedBuffer;
import com.baidu.cloud.thirdparty.dyuprotostuff.ProtobufIOUtil;
import com.baidu.cloud.thirdparty.dyuprotostuff.Schema;
import com.baidu.cloud.thirdparty.dyuprotostuff.runtime.DefaultIdStrategy;
import com.baidu.cloud.thirdparty.dyuprotostuff.runtime.IdStrategy;
import com.baidu.cloud.thirdparty.dyuprotostuff.runtime.RuntimeSchema;
import com.baidu.cloud.starlight.api.exception.CodecException;
import com.baidu.cloud.starlight.api.serialization.serializer.Serializer;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Since Stargate uses the old version of Protostuff and is not compatible with the new version, we use two Serializers
 * for processing stargate ser/deser Created by liuruisen on 2020/9/1.
 */
public class DyuProtostuffSerializer implements Serializer {

    private static final int DEFAULT_ALLOCATE_NUM = 512;
    // Re-use (manage) this buffer to avoid allocating on every serialization
    private ThreadLocal<LinkedBuffer> buffer = new ThreadLocal<LinkedBuffer>() {
        @Override
        protected LinkedBuffer initialValue() {
            return LinkedBuffer.allocate(DEFAULT_ALLOCATE_NUM);
        }
    };

    private Map<ClassLoader, IdStrategy> idStrategyMap = new ConcurrentHashMap<>();

    @Override
    public byte[] serialize(Object obj, Type type) throws CodecException {
        if (obj == null) {
            throw new CodecException(CodecException.SERIALIZE_EXCEPTION,
                "The message used to dyuprotostuff-serialize is null");
        }

        if (!(type instanceof Class)) {
            throw new CodecException(CodecException.SERIALIZE_EXCEPTION,
                "The type to dyuprotostuff-serialize is not Class");
        }

        Class clazz = (Class) type;
        if (!isSupported(clazz)) {
            throw new CodecException(CodecException.SERIALIZE_EXCEPTION,
                "The type {" + type + "} used to dyuprotostuff-deserialize is illegal");
        }

        long starTime = System.currentTimeMillis();
        // use predefine object container as default
        try {
            Schema schema = RuntimeSchema.getSchema(clazz, getIdStrategy());
            byte[] result = ProtobufIOUtil.toByteArray(obj, schema, buffer.get());
            LOGGER.debug("Serialize obj cost: {}", System.currentTimeMillis() - starTime);
            return result;
        } catch (Exception e) {
            throw new CodecException(CodecException.SERIALIZE_EXCEPTION,
                "DyuProtostuff serialize error: " + e.getMessage(), e);
        } finally {
            buffer.get().clear();
        }
    }

    @Override
    public Object deserialize(byte[] bytes, Type type) throws CodecException {
        if (bytes == null || bytes.length == 0) {
            throw new CodecException(CodecException.DESERIALIZE_EXCEPTION,
                "The bytes used to dyuprotostuff deserializing are empty");
        }

        if (!(type instanceof Class)) {
            throw new CodecException(CodecException.DESERIALIZE_EXCEPTION,
                "The type to dyuprotostuff deserializing is not Class");
        }

        Class clazz = (Class) type;
        if (!isSupported(clazz)) {
            throw new CodecException(CodecException.DESERIALIZE_EXCEPTION,
                "The type used to dyuprotostuff deserializing are illegal");
        }
        long starTime = System.currentTimeMillis();
        // use predefine object container as default
        try {
            Schema schema = RuntimeSchema.getSchema(clazz, getIdStrategy());
            Object content = schema.newMessage();
            ProtobufIOUtil.mergeFrom(bytes, content, schema);
            LOGGER.debug("Serialize obj cost: {}", System.currentTimeMillis() - starTime);
            return content;
        } catch (Exception e) {
            throw new CodecException(CodecException.DESERIALIZE_EXCEPTION,
                "DyuProtostuff Deserialize error: " + e.getMessage(), e);
        }
    }

    private boolean isSupported(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        if (clazz.isPrimitive()) {
            return false;
        }
        return true;
    }

    private IdStrategy getIdStrategy() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        IdStrategy strategy = idStrategyMap.get(classLoader);
        if (strategy == null) {
            idStrategyMap.putIfAbsent(classLoader, new DefaultIdStrategy());
            strategy = idStrategyMap.get(classLoader);
        }
        return strategy;
    }
}
