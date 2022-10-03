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
 
package com.baidu.cloud.starlight.core.utils;

import com.baidu.cloud.thirdparty.jackson.databind.JsonNode;
import com.baidu.cloud.thirdparty.jackson.databind.ObjectMapper;
import com.baidu.cloud.thirdparty.jackson.databind.node.ArrayNode;
import com.baidu.cloud.thirdparty.jackson.databind.node.ObjectNode;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import static com.baidu.cloud.thirdparty.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

/**
 * Used in generic filter. When filter request, convert generic args to json strings; when filter response, convert json
 * strings to real objects.
 */
public class PojoJsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        // 设置输入时忽略在JSON字符串中存在但Java对象实际没有的属性
        OBJECT_MAPPER.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 默认时间格式
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        OBJECT_MAPPER.setDateFormat(fmt);
    }

    /**
     * Map or List to Object
     *
     * @param args
     * @param types
     * @return
     * @throws Exception
     */
    public static Object[] realize(Object[] args, Type[] types) throws Exception {
        if (args == null) {
            return args;
        }
        if (args.length != types.length) {
            throw new IllegalArgumentException("args.length != types.length");
        }
        Object[] result = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg == null) {
                continue;
            }
            if (arg instanceof String) {
                if (String.class.equals(types[i])) {
                    result[i] = arg;
                    continue;
                }
                result[i] = OBJECT_MAPPER.readValue((String) arg, OBJECT_MAPPER.constructType(types[i]));
            } else {
                String json = OBJECT_MAPPER.writeValueAsString(arg);
                result[i] = OBJECT_MAPPER.readValue(json, OBJECT_MAPPER.constructType(types[i]));
            }
        }
        return result;
    }

    /**
     * Object to Map or List
     *
     * @param data
     * @return
     * @throws Exception
     */
    public static Object generalize(Object data) throws Exception {
        if (data == null) {
            return data;
        }
        String json = OBJECT_MAPPER.writeValueAsString(data);
        JsonNode jsonNode = OBJECT_MAPPER.readTree(json);
        if (jsonNode instanceof ObjectNode) {
            return OBJECT_MAPPER.readValue(json, Map.class);
        } else if (jsonNode instanceof ArrayNode) {
            return OBJECT_MAPPER.readValue(json, List.class);
        } else {
            return data;
        }
    }

    public static Object[] toJsonString(Object[] args) throws Exception {
        Object[] result = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            result[i] = OBJECT_MAPPER.writeValueAsString(args[i]);
        }
        return result;
    }
}