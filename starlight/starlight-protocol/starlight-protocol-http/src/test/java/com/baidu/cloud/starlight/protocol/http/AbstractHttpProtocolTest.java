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
 
package com.baidu.cloud.starlight.protocol.http;

import io.netty.handler.codec.http.FullHttpRequest;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.protocol.ProtocolDecoder;
import com.baidu.cloud.starlight.api.protocol.ProtocolEncoder;
import com.baidu.cloud.starlight.serialization.serializer.JsonSerializer;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Created by liuruisen on 2020/5/28.
 */
public class AbstractHttpProtocolTest {

    private AbstractHttpProtocol protocol = new AbstractHttpProtocol() {
        @Override
        public ProtocolEncoder getEncoder() {
            return new HttpEncoder() {
                @Override
                protected FullHttpRequest convertRequest(Request rpcRequest) {
                    return null;
                }
            };
        }

        @Override
        public ProtocolDecoder getDecoder() {
            return new HttpDecoder() {
                @Override
                protected Request reverseConvertRequest(FullHttpRequest httpRequest) {
                    return null;
                }
            };
        }
    };

    @Test
    public void getEncoder() {
        assertTrue(protocol.getEncoder() instanceof HttpEncoder);
    }

    @Test
    public void getDecoder() {
        assertTrue(protocol.getDecoder() instanceof HttpDecoder);
    }

    @Test
    public void getHeartbeatTrigger() {
        assertTrue(protocol.getHeartbeatTrigger() == null);
    }

    @Test
    public void getSerialize() {
        assertTrue(protocol.getSerialize() instanceof JsonSerializer);
    }
}