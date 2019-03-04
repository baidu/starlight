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
package com.baidu.brpc.protocol;

import io.netty.util.concurrent.FastThreadLocal;

/**
 * Bprc http response implementation, used for http protocols.
 * So far, we use netty {@link io.netty.handler.codec.http.HttpClientCodec}
 * and {@link io.netty.handler.codec.http.HttpServerCodec}
 * and {@link io.netty.handler.codec.http.HttpObjectAggregator} to handle http message.
 *
 * @author wangjiayin@baidu.com
 * @see com.baidu.brpc.client.RpcClient
 * @see com.baidu.brpc.server.RpcServer
 * @since 2018-12-26
 */
public class HttpResponse extends AbstractResponse {

    private static final FastThreadLocal<HttpResponse> CURRENT_RPC_RESPONSE = new FastThreadLocal<HttpResponse>() {
        @Override
        protected HttpResponse initialValue() {
            return new HttpResponse();
        }
    };

    public static HttpResponse getHttpResponse() {
        return CURRENT_RPC_RESPONSE.get();
    }

}
