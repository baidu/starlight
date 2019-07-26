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

package com.baidu.brpc.interceptor;

import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;

/**
 * Interceptor for client side tracing.
 * <p>
 * This interceptor is just a placeholder for integration with tracing tools using AOP.
 */
public class ClientTraceInterceptor extends AbstractInterceptor {
    @Override
    public boolean handleRequest(Request request) {
        return super.handleRequest(request);
    }

    @Override
    public void handleResponse(Response response) {
        super.handleResponse(response);
    }

    @Override
    public void aroundProcess(Request request, Response response, InterceptorChain chain) throws Exception {
        super.aroundProcess(request, response, chain);
    }
}
