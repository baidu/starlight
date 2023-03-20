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
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtobufIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.DefaultIdStrategy;
import io.protostuff.runtime.IdStrategy;
import io.protostuff.runtime.RuntimeSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Support serialize pojo at runtime , Used to serializer/deserializer in runtime without defining proto file Created by
 * liuruisen on 2020/2/20.
 */
public class ProtoStuffSerializer implements Serializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProtoStuffSerializer.class);

    private static final int DEFAULT_ALLOCATE_NUM = 512;
    // Re-use (manage) this buffer to avoid allocating on every serialization
    private ThreadLocal<LinkedBuffer> buffer = new ThreadLocal<LinkedBuffer>() {
        @Override
        protected LinkedBuffer initialValue() {
            return LinkedBuffer.allocate(DEFAULT_ALLOCATE_NUM);
        }
    };

    private final Map<ClassLoader, ConcurrentHashMap<Integer, IdStrategy>> idStrategyMap = new ConcurrentHashMap<>();

    @Override
    public byte[] serialize(Object obj, Type type) throws CodecException {
        return serialize(obj, type, IdStrategy.DEFAULT_FLAGS);
    }

    /**
     * Support specified idstrategy, used in brpc
     *
     * @param obj
     * @param type
     * @param strategyFlag
     * @return
     * @throws CodecException
     */
    public byte[] serialize(Object obj, Type type, Integer strategyFlag) throws CodecException {

        if (obj == null) {
            throw new CodecException(CodecException.SERIALIZE_EXCEPTION,
                "The message used to protostuff-serialize is null");
        }

        if (!(type instanceof Class)) {
            throw new CodecException(CodecException.SERIALIZE_EXCEPTION,
                "The type to protostuff-serialize is not Class");
        }

        Class clazz = (Class) type;
        if (!isSupported(clazz)) {
            throw new CodecException(CodecException.SERIALIZE_EXCEPTION,
                "The type {" + type + "} used to protostuff-deserialize is illegal");
        }

        long starTime = System.currentTimeMillis();
        // use predefine object container as default
        try {
            Schema schema = RuntimeSchema.getSchema(clazz, getIdStrategy(strategyFlag)); // schema will be cached
            byte[] result = ProtobufIOUtil.toByteArray(obj, schema, buffer.get());
            LOGGER.debug("Serialize obj cost: {}", System.currentTimeMillis() - starTime);
            return result;
        } catch (Throwable e) {
            CodecException exception = SerializationUtils.convertToCodecException(e, CodecException.SERIALIZE_EXCEPTION,
                "Protostuff serialize error");
            LOGGER.error("Serialize error, ", e);
            throw exception;
        } finally {

            buffer.get().clear();
        }
    }

    @Override
    public Object deserialize(byte[] bytes, Type type) throws CodecException {

        return deserialize(bytes, type, IdStrategy.DEFAULT_FLAGS);
    }

    /**
     * Support specified idstrategy, used in brpc
     *
     * @param bytes
     * @param type
     * @param strategyFlag
     * @return
     * @throws CodecException
     */
    public Object deserialize(byte[] bytes, Type type, Integer strategyFlag) throws CodecException {

        if (bytes == null || bytes.length == 0) {
            throw new CodecException(CodecException.DESERIALIZE_EXCEPTION,
                "The bytes used to protostuff deserializing are empty");
        }

        if (!(type instanceof Class)) {
            throw new CodecException(CodecException.DESERIALIZE_EXCEPTION,
                "The type to protostuff deserializing is not Class");
        }

        Class clazz = (Class) type;
        if (!isSupported(clazz)) {
            throw new CodecException(CodecException.DESERIALIZE_EXCEPTION,
                "The type used to protostuff deserializing are illegal");
        }
        long starTime = System.currentTimeMillis();
        // use predefine object container as default
        try {
            Schema schema = RuntimeSchema.getSchema(clazz, getIdStrategy(strategyFlag)); // schema will be cached
            Object content = schema.newMessage();
            ProtobufIOUtil.mergeFrom(bytes, content, schema);
            LOGGER.debug("Deserialize obj cost: {}", System.currentTimeMillis() - starTime);
            return content;
        } catch (Throwable e) {
            CodecException exception = SerializationUtils.convertToCodecException(e,
                CodecException.DESERIALIZE_EXCEPTION, "Protostuff Deserialize error");
            LOGGER.error("Deserialize error, ", e);
            throw exception;
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

    /**
     * 将IdStrategy与ClassLoader关联起来 其原因是， <1> IdStrategy中缓存了很多类的Schema信息，这些信息与ClassLoader关联的 </1> <2>
     * hairuo场景中存在多个ClassLoader的场景 </2> 因而为适应海若场景的使用，需要做关联
     * 
     * @param strategyFlag
     * @return
     */
    private IdStrategy getIdStrategy(Integer strategyFlag) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Map<Integer, IdStrategy> strategies = idStrategyMap.get(classLoader);

        if (strategies == null) {
            idStrategyMap.putIfAbsent(classLoader, new ConcurrentHashMap<>());
            strategies = idStrategyMap.get(classLoader);
        }

        IdStrategy idStrategy = strategies.get(strategyFlag);

        if (idStrategy == null) {
            // 重点为此处，通过new DefaultIdStrategy的方式，将classloader与DefaultIdStrategy关联起来
            strategies.putIfAbsent(strategyFlag, new DefaultIdStrategy(strategyFlag));
            idStrategy = strategies.get(strategyFlag);
        }

        return idStrategy;
    }
}
