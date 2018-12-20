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

import com.baidu.brpc.client.RpcFuture;
import com.baidu.brpc.RpcMethodInfo;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.concurrent.FastThreadLocal;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * 反序列化之后的返回信息。
 */
@Setter
@Getter
public class RpcResponse extends DefaultFullHttpResponse {
    private static final FastThreadLocal<RpcResponse> CURRENT_RPC_RESPONSE = new FastThreadLocal<RpcResponse>() {
        @Override
        protected RpcResponse initialValue() {
            return new RpcResponse();
        }
    };

    public static RpcResponse getRpcResponse() {
        return CURRENT_RPC_RESPONSE.get();
    }

    private long logId;
    private Object result;
    private Throwable exception;
    private RpcMethodInfo rpcMethodInfo;
    private RpcFuture rpcFuture;
    private Map<String, String> kvAttachment = new HashMap<String, String>();
    private ByteBuf binaryAttachment;
    private int compressType;

    public RpcResponse() {
        super(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
    }

    /**
     * http response引用计数交给rpc response管理。
     * @param httpResponse http response
     */
    public RpcResponse(FullHttpResponse httpResponse) {
        super(httpResponse.protocolVersion(), httpResponse.status(), httpResponse.content(),
                httpResponse.headers().copy(), httpResponse.trailingHeaders().copy());
        this.setDecoderResult(httpResponse.decoderResult());
    }

    public void setHttpResponse(FullHttpResponse httpResponse) {
        setProtocolVersion(httpResponse.protocolVersion());
        setStatus(httpResponse.status());
        content().writeBytes(httpResponse.content());
        headers().add(httpResponse.headers());
        trailingHeaders().add(httpResponse.trailingHeaders());
        setDecoderResult(httpResponse.decoderResult());
    }

    public void reset() {
        logId = -1;
        result = null;
        exception = null;
        rpcMethodInfo = null;
        rpcFuture = null;
        kvAttachment.clear();
        delRefCntForServer();
        compressType = 0;
        setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
        content().clear();
        headers().clear();
        trailingHeaders().clear();
    }

    public void delRefCntForServer() {
        if (binaryAttachment != null) {
            int refCnt = binaryAttachment.refCnt();
            if (refCnt > 0) {
                binaryAttachment.release();
            }
            binaryAttachment = null;
        }
    }

    public void delRefCntForClient() {
        if (super.refCnt() > 0) {
            super.release();
        }
    }
}
