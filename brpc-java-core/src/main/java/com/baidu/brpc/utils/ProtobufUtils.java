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

package com.baidu.brpc.utils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;
import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;
import com.baidu.brpc.buffer.DynamicCompositeByteBuf;
import com.google.protobuf.Message;

import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProtobufUtils {
    public enum MessageType {
        PROTOBUF,
        JPROTOBUF,
        POJO
    }

    public static MessageType getMessageType(Method method) {
        Class<?>[] types = method.getParameterTypes();
        Class returnType = method.getReturnType();
        if (types.length < 0) {
            throw new IllegalArgumentException("invalid rpc method params");
        }

        if (types.length != 1) {
            return MessageType.POJO;
        }

        Class<?> inputType = types[0];
        if (Message.class.isAssignableFrom(inputType)
                && Message.class.isAssignableFrom(returnType)) {
            return MessageType.PROTOBUF;
        }

        ProtobufClass protobufClass = inputType.getAnnotation(ProtobufClass.class);
        if (protobufClass != null) {
            return MessageType.JPROTOBUF;
        }

        Field[] fields = inputType.getFields();
        for (Field field : fields) {
            Protobuf protobuf = field.getAnnotation(Protobuf.class);
            if (protobuf != null) {
                return MessageType.JPROTOBUF;
            }
        }

        return MessageType.POJO;
    }

    public static Message parseFrom(InputStream inputStream, Class clazz) {
        try {
            Method method = clazz.getMethod("getDefaultInstance");
            Message proto = (Message) method.invoke(null);
            proto = proto.newBuilderForType().mergeFrom(inputStream).build();
            return proto;
        } catch (Exception ex) {
            String errorMsg = String.format("parse proto failed, msg=%s", ex.getMessage());
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }
    }

    public static Message parseFrom(byte[] inputBytes, Class clazz) {
        try {
            Method method = clazz.getMethod("parseFrom", byte[].class);
            Message proto = (Message) method.invoke(null, inputBytes);
            return proto;
        } catch (Exception ex) {
            String errorMsg = String.format("parse proto failed, msg=%s", ex.getMessage());
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }
    }

    /**
     * parse proto from netty {@link ByteBuf}
     * @param input netty ByteBuf
     * @param defaultInstance default instance for proto
     * @return proto message
     * @throws IOException read io exception
     */
    public static Message parseFrom(ByteBuf input, Message defaultInstance) throws IOException {
        final int length = input.readableBytes();
        byte[] array = new byte[length];
        input.readBytes(array, 0, length);
        return defaultInstance.getParserForType().parseFrom(array);
    }

    public static Message parseFrom(byte[] input, Message defaultInstance) throws IOException {
        return defaultInstance.getParserForType().parseFrom(input);
    }

    public static Message parseFrom(DynamicCompositeByteBuf input, Message defaultInstance) throws IOException {
        final byte[] array;
        final int offset;
        final int length = input.readableBytes();
        array = new byte[length];
        input.readBytes(array, 0, length);
        offset = 0;
        return defaultInstance.getParserForType().parseFrom(array, offset, length);
    }
}
