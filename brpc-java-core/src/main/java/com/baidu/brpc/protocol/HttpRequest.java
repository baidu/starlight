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

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.concurrent.FastThreadLocal;
import lombok.Getter;
import lombok.Setter;

/**
 * Bprc http request implementation, used for http protocols.
 * So far, we use netty {@link io.netty.handler.codec.http.HttpClientCodec}
 * and {@link io.netty.handler.codec.http.HttpServerCodec}
 * and {@link io.netty.handler.codec.http.HttpObjectAggregator} to handle http message.
 *
 * @author wangjiayin@baidu.com
 * @see com.baidu.brpc.client.RpcClient
 * @see com.baidu.brpc.server.RpcServer
 * @since 2018-12-26
 */
@Getter
@Setter
public class HttpRequest extends AbstractRequest {

    private static final FastThreadLocal<HttpRequest> CURRENT_RPC_REQUEST = new FastThreadLocal<HttpRequest>() {
        @Override
        protected HttpRequest initialValue() {
            return new HttpRequest();
        }
    };

    public static HttpRequest getHttpRequest() {
        return CURRENT_RPC_REQUEST.get();
    }

    private FullHttpRequest nettyHttpRequest;

    public HttpRequest() {
        this.nettyHttpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "");
    }

    @Override
    public Object getMsg() {
        return nettyHttpRequest;
    }

    @Override
    public void setMsg(Object o) {
        FullHttpRequest request = (FullHttpRequest) o;
        nettyHttpRequest.setProtocolVersion(request.protocolVersion());
        nettyHttpRequest.headers().add(request.headers());
        nettyHttpRequest.setMethod(request.method());
        nettyHttpRequest.setUri(request.uri());
        nettyHttpRequest.content().writeBytes(request.content());
        nettyHttpRequest.trailingHeaders().add(request.trailingHeaders());
        nettyHttpRequest.setDecoderResult(request.decoderResult());
    }

    @Override
    public void reset() {
        super.reset();
        nettyHttpRequest.setUri("");
        nettyHttpRequest.content().clear();
        nettyHttpRequest.headers().clear();
        nettyHttpRequest.trailingHeaders().clear();
    }

    @Override
    public Request retain() {
        super.retain();
        nettyHttpRequest.retain();
        return this;
    }

    @Override
    public void release() {
        super.release();
        if (nettyHttpRequest.refCnt() > 0) {
            nettyHttpRequest.release();
        }
    }

    public HttpHeaders headers() {
        return nettyHttpRequest.headers();
    }

    public String uri() {
        return nettyHttpRequest.uri();
    }

    public ByteBuf content() {
        return nettyHttpRequest.content();
    }
}
