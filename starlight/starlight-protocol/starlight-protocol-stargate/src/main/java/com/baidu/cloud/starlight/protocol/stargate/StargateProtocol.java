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
 
package com.baidu.cloud.starlight.protocol.stargate;

import com.baidu.cloud.starlight.api.protocol.HeartbeatTrigger;
import com.baidu.cloud.starlight.api.protocol.Protocol;
import com.baidu.cloud.starlight.api.protocol.ProtocolDecoder;
import com.baidu.cloud.starlight.api.protocol.ProtocolEncoder;
import com.baidu.cloud.starlight.api.serialization.serializer.Serializer;
import com.baidu.cloud.starlight.serialization.serializer.DyuProtostuffSerializer;

/**
 * SPI name: stargate Created by liuruisen on 2020/7/20.
 */
public class StargateProtocol implements Protocol {

    static {
        System.setProperty("protostuff.runtime.collection_schema_on_repeated_fields", "true");
        System.setProperty("protostuff.runtime.morph_collection_interfaces", "true");
        System.setProperty("protostuff.runtime.morph_map_interfaces", "true");
    }

    public static final String PROTOCOL_NAME = "stargate"; // stargate protocol name star

    protected static final int HEAD_LEN = 4;

    private static final DyuProtostuffSerializer serializer = new DyuProtostuffSerializer();

    private static final StargateEncoder encoder = new StargateEncoder();

    private static final StargateDecoder decoder = new StargateDecoder();

    /**
     * stargate header size is 4, first byte of body is 10d(key = 1, type Length-delimited), see
     * <a href="https://linghutf.github.io/2016/06/08/protobuf/">protobuf key definition</a>
     */
    protected static final int FIXED_LEN = HEAD_LEN + 1;

    /**
     * stargate first byte value of body is 10. protobuf key 定义 (field_number &lt;&lt; 3) | wire_type 0000 1010 ==&gt;
     * 000 1010 (去掉最高位） ==&gt; 0001 --&gt; key 1 ==&gt; 010 --&gt; wire_type 2(Length-delimited)
     */
    protected static final int FIRST_BYTE_VALUE_OF_BODY = 10;

    protected static final int MAX_BODY_SIZE = 512 * 1024 * 1024; // 512M

    @Override
    public ProtocolEncoder getEncoder() {
        return encoder;
    }

    @Override
    public ProtocolDecoder getDecoder() {
        return decoder;
    }

    @Override
    public HeartbeatTrigger getHeartbeatTrigger() {
        return null;
    }

    @Override
    public Serializer getSerialize() {
        return serializer;
    }
}
