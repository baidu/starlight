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
 
package com.baidu.cloud.starlight.protocol.http.springrest;

import com.baidu.cloud.starlight.api.protocol.ProtocolDecoder;
import com.baidu.cloud.starlight.api.protocol.ProtocolEncoder;
import com.baidu.cloud.starlight.protocol.http.AbstractHttpProtocol;

/**
 * Allow developer to use spring rest annotation to develop Restful API. SpringRestProtocol will parse spring rest
 * annotation metadata and convert method call to http request. SPI name: springrest TODO 尚未实现heartbeat Created by
 * liuruisen on 2020/5/27.
 */
public class SpringRestProtocol extends AbstractHttpProtocol {

    public static final String PROTOCOL_NAME = "springrest";

    private static final SpringRestHttpEncoder encoder = new SpringRestHttpEncoder();

    private static final SpringRestHttpDecoder decoder = new SpringRestHttpDecoder();

    @Override
    public ProtocolEncoder getEncoder() {
        return encoder;
    }

    @Override
    public ProtocolDecoder getDecoder() {
        return decoder;
    }
}
