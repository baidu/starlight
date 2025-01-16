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
import com.baidu.cloud.thirdparty.jackson.annotation.JsonInclude;
import com.baidu.cloud.thirdparty.jackson.databind.DeserializationFeature;
import com.baidu.cloud.thirdparty.jackson.databind.JavaType;
import com.baidu.cloud.thirdparty.jackson.databind.ObjectMapper;
import com.baidu.cloud.thirdparty.jackson.databind.SerializationFeature;
import com.baidu.cloud.thirdparty.jackson.databind.type.TypeFactory;
import com.baidu.cloud.thirdparty.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;

/**
 * Json Serializer based on jackson. Created by liuruisen on 2020/5/27.
 */
public class JsonSerializer implements Serializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonSerializer.class);

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        // 序列化的时候序列对象的所有属性
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.ALWAYS);

        // 反序列化的时候如果多了其他属性,不抛出异常
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 如果是空对象的时候,不抛异常
        OBJECT_MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        // 取消时间的转化格式,默认是时间戳,可以取消,同时需要设置要表现的时间格式
        OBJECT_MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        OBJECT_MAPPER.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
    }

    @Override
    public byte[] serialize(Object obj, Type type) throws CodecException {
        if (obj == null) {
            throw new CodecException(CodecException.SERIALIZE_EXCEPTION, "The message used to json-serialize is null");
        }
        try {
            return OBJECT_MAPPER.writeValueAsBytes(obj); // json string + utf8 encode
        } catch (Throwable e) {
            LOGGER.error("JsonSerializer bodySerialize fail: ", e);
            throw new CodecException(CodecException.SERIALIZE_EXCEPTION, "Json serialize error: " + e.getMessage(), e);
        }
    }

    @Override
    public Object deserialize(byte[] bytes, Type type) throws CodecException {

        if (bytes == null || bytes.length == 0) {
            throw new CodecException(CodecException.DESERIALIZE_EXCEPTION,
                "The bytes used to json deserialize are empty");
        }

        try {
            return OBJECT_MAPPER.readValue(bytes, getJavaType(type));
        } catch (Throwable e) {
            LOGGER.error("JsonSerializer bodyDeserialize fail: ", e);
            throw new CodecException(CodecException.DESERIALIZE_EXCEPTION, "Json deserialize error: " + e.getMessage(),
                e);
        }
    }

    private JavaType getJavaType(Type type) {
        TypeFactory typeFactory = OBJECT_MAPPER.getTypeFactory();
        return typeFactory.constructType(type);
    }

}
