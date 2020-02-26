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

import com.baidu.brpc.client.CommunicationClient;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import lombok.Setter;

/**
 * invoke rpc.
 * this interceptor is the last one of client interceptor list.
 */
@Setter
public class ClientInvokeInterceptor extends AbstractInterceptor {
    protected CommunicationClient communicationClient;

    public ClientInvokeInterceptor(CommunicationClient client) {
        this.communicationClient = client;
    }

    @Override
    public void aroundProcess(Request request, Response response, InterceptorChain chain) throws RpcException {
        try {
            communicationClient.execute(request, response);
        } finally {
            chain.intercept(request, response);
        }
    }
}
