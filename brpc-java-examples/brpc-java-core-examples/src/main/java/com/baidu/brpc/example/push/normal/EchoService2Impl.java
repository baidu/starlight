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

package com.baidu.brpc.example.push.normal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EchoService2Impl implements EchoService2 {
    private static final Logger LOG = LoggerFactory.getLogger(EchoService2Impl.class);
    public static volatile boolean client2Started = false;

    @Override
    public EchoResponse echo(EchoRequest request) {
        String message = request.getMessage();
        EchoResponse response = new EchoResponse();
        response.setMessage(message);
        LOG.debug("EchoService.echo, request={}, response={}",
                request.getMessage(), response.getMessage());
        client2Started = true;
        return response;
    }
}
