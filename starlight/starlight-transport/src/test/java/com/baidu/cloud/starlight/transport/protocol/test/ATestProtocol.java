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
 
package com.baidu.cloud.starlight.transport.protocol.test;

import com.baidu.cloud.starlight.api.protocol.HeartbeatTrigger;
import com.baidu.cloud.starlight.api.protocol.Protocol;
import com.baidu.cloud.starlight.api.protocol.ProtocolDecoder;
import com.baidu.cloud.starlight.api.protocol.ProtocolEncoder;
import com.baidu.cloud.starlight.api.serialization.serializer.Serializer;
import com.baidu.cloud.starlight.serialization.serializer.ProtoStuffSerializer;

public class ATestProtocol implements Protocol {
    protected static final String PROTOCOL_NAME = "atest";
    protected static final Integer FIXED_LEN = 16;
    protected static final byte[] MAGIC_HEAD = "Test".getBytes();

    @Override
    public ProtocolEncoder getEncoder() {
        return null;
    }

    @Override
    public ProtocolDecoder getDecoder() {
        return new TestDecoder();
    }

    @Override
    public HeartbeatTrigger getHeartbeatTrigger() {
        return new TestHeartbeatTrigger();
    }

    @Override
    public Serializer getSerialize() {
        return new ProtoStuffSerializer();
    }
}