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

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import com.baidu.brpc.RpcContext;
import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.client.RpcCallback;
import com.baidu.brpc.client.channel.BrpcChannel;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.protocol.nshead.NSHead;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public abstract class AbstractRequest implements Request {
    private Object msg;
    private long logId;
    private Object target;
    private Method targetMethod;
    private RpcMethodInfo rpcMethodInfo;
    private String serviceName;
    private String methodName;
    private Object[] args;
    private NSHead nsHead;
    private Map<String, String> kvAttachment;
    private ByteBuf binaryAttachment;
    private int compressType;
    private RpcException exception;
    private Channel channel;
    private Set<BrpcChannel> selectedInstances;
    private String auth;
    private Long traceId;
    private Long spanId;
    private Long parentSpanId;
    private RpcContext rpcContext;
    private RpcCallback callback;

    @Override
    public void reset() {
        msg = null;
        logId = -1;
        target = null;
        targetMethod = null;
        rpcMethodInfo = null;
        serviceName = "";
        methodName = "";
        args = null;
        nsHead = null;
        kvAttachment = null;
        binaryAttachment = null;
        compressType = 0;
        exception = null;
        channel = null;
        selectedInstances = null;
        traceId = null;
        spanId = null;
        parentSpanId = null;
        rpcContext = null;
        callback = null;
    }

    @Override
    public Request retain() {
        if (binaryAttachment != null) {
            binaryAttachment.retain();
        }
        return this;
    }

    @Override
    public void release() {
        if (binaryAttachment != null && binaryAttachment.refCnt() > 0) {
            binaryAttachment.release();
            binaryAttachment = null;
        }
    }
}
