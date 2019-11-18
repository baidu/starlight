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

package com.baidu.brpc.example.springboot.server;

import com.baidu.brpc.interceptor.AbstractInterceptor;
import com.baidu.brpc.interceptor.InterceptorChain;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomInterceptor2 extends AbstractInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(CustomInterceptor2.class);

    public boolean handleRequest(Request rpcRequest) {
        LOG.info("request intercepted by CustomInterceptor2, correlationId={}, service={}, method={}",
                rpcRequest.getCorrelationId(),
                rpcRequest.getTarget().getClass().getSimpleName(),
                rpcRequest.getTargetMethod().getName());
        return true;
    }

    @Override
    public void aroundProcess(Request request, Response response, InterceptorChain chain) throws Exception {
        LOG.info("around intercepted, before proceed, correlationId={}, service={}, method={}",
                request.getCorrelationId(),
                request.getTarget().getClass().getSimpleName(),
                request.getTargetMethod().getName());

        // invoke the interceptor list
        chain.intercept(request, response);

        LOG.info("around intercepted, after proceed, correlationId={}, service={}, method={}",
                request.getCorrelationId(),
                request.getTarget().getClass().getSimpleName(),
                request.getTargetMethod().getName());
    }

    public void handleResponse(Response response) {
        if (response != null) {
            LOG.info("reponse intercepted, correlationId={}, result={}",
                    response.getCorrelationId(), response.getResult());
        }
    }
}
