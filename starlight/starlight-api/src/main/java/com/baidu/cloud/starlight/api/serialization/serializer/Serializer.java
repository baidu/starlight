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
 
package com.baidu.cloud.starlight.api.serialization.serializer;

import com.baidu.cloud.starlight.api.exception.CodecException;
import com.baidu.cloud.starlight.api.protocol.ProtocolDecoder;
import com.baidu.cloud.starlight.api.protocol.ProtocolEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;

/**
 * Serialize param obj to bytes, or deserialize bytes to param obj. Only serialize or deserialize, Encode see
 * {@link ProtocolEncoder} Decode see {@link ProtocolDecoder} Supported SPI Created by liuruisen on 2019/12/4.
 */
public interface Serializer {

    Logger LOGGER = LoggerFactory.getLogger(Serializer.class);

    String DESERIALIZE_ERROR_MSG =
        "The problem is usually caused by" + "\n 1: difference of api.jar between server and client."
            + "\n 2: API contains a type that Starlight(stargate) does not support. eg:HashMap.keySet()"
            + "\n see http://wiki.baidu.com/pages/viewpage.action?pageId=1348583072 "
            + "to get more information about incompatible APIs";

    byte[] serialize(Object obj, Type type) throws CodecException;

    Object deserialize(byte[] bytes, Type type) throws CodecException;
}
