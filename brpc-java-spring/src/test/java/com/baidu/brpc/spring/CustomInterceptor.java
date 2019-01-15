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

package com.baidu.brpc.spring;

import com.baidu.brpc.protocol.RpcRequest;
import com.baidu.brpc.protocol.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.brpc.interceptor.Interceptor;

public class CustomInterceptor implements Interceptor {
    private static final Logger LOG = LoggerFactory.getLogger(CustomInterceptor.class);

    public boolean handleRequest(RpcRequest rpcRequest) {
        LOG.info("request intercepted, logId={}, service={}, method={}",
                rpcRequest.getLogId(),
                rpcRequest.getTarget().getClass().getSimpleName(),
                rpcRequest.getTargetMethod().getName());
        return true;
    }

    public void handleResponse(RpcResponse response) {
        if (response != null) {
            LOG.info("reponse intercepted, logId={}, result={}",
                    response.getLogId(), response.getResult());
        }
    }
}
