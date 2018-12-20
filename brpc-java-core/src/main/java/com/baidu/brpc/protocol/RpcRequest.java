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

import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.protocol.nshead.NSHeadMeta;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.util.concurrent.FastThreadLocal;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 序列化之前的请求调用信息，用于传给interceptor。
 */
@Setter
@Getter
public class RpcRequest extends DefaultFullHttpRequest {
    private static final FastThreadLocal<RpcRequest> CURRENT_RPC_REQUEST = new FastThreadLocal<RpcRequest>() {
        @Override
        protected RpcRequest initialValue() {
            return new RpcRequest();
        }
    };

    public static RpcRequest getRpcRequest() {
        return CURRENT_RPC_REQUEST.get();
    }

    private long logId;
    private Object target;
    private Method targetMethod;
    private RpcMethodInfo rpcMethodInfo;
    private String serviceName;
    private String methodName;
    private Object[] args;
    private NSHeadMeta nsHeadMeta;
    private Map<String, String> kvAttachment = new HashMap<String, String>();
    private ByteBuf binaryAttachment;
    private int compressType;
    private RpcException exception;
    private Channel channel; // 在server端保存client的channel信息

    public RpcRequest() {
        super(HttpVersion.HTTP_1_1, HttpMethod.POST, "");
    }

    /**
     * httpRequest引用计数交给RpcRequest管理
     * @param request http request
     */
    public RpcRequest(FullHttpRequest request) {
        super(request.protocolVersion(), request.method(), request.uri(), request.content(),
                request.headers().copy(), request.trailingHeaders().copy());
        setDecoderResult(decoderResult());
    }

    public void setHttpRequest(FullHttpRequest request) {
        setProtocolVersion(request.protocolVersion());
        headers().add(request.headers());
        setMethod(request.method());
        setUri(request.uri());
        content().writeBytes(request.content());
        trailingHeaders().add(request.trailingHeaders());
        setDecoderResult(request.decoderResult());
    }

    public void reset() {
        logId = -1;
        target = null;
        targetMethod = null;
        rpcMethodInfo = null;
        serviceName = "";
        methodName = "";
        args = null;
        nsHeadMeta = null;
        kvAttachment.clear();
        delRefCntForServer();
        compressType = 0;
        exception = null;
        channel = null;
        setUri("");
        content().clear();
        headers().clear();
        trailingHeaders().clear();
    }

    public RpcRequest addRefCnt() {
        super.retain();
        if (binaryAttachment != null) {
            binaryAttachment.retain();
        }
        return this;
    }

    public void delRefCnt() {
        if (super.refCnt() > 0) {
            super.release(refCnt());
        }
        if (binaryAttachment != null && binaryAttachment.refCnt() > 0) {
            binaryAttachment.release();
        }
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
}
