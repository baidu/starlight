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
 
package com.baidu.cloud.starlight.protocol.http.springrest.sse;

import com.baidu.cloud.starlight.api.protocol.ProtocolDecoder;
import com.baidu.cloud.starlight.api.protocol.ProtocolEncoder;
import com.baidu.cloud.starlight.protocol.http.springrest.SpringRestProtocol;

public class SpringRestSseProtocol extends SpringRestProtocol {

    public static final String PROTOCOL_NAME = "springrestsse";

    private static final SpringRestSseHttpEncoder ENCODER = new SpringRestSseHttpEncoder();

    private static final SpringRestSseHttpDecoder DECODER = new SpringRestSseHttpDecoder();

    @Override
    public ProtocolEncoder getEncoder() {
        return ENCODER;
    }

    @Override
    public ProtocolDecoder getDecoder() {
        return DECODER;
    }
}
