/*
 * Copyright (C) 2019 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.brpc.protocol;

import java.util.Map;

import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.client.RpcFuture;
import com.baidu.brpc.protocol.nshead.NSHead;
import com.baidu.brpc.protocol.push.SPHead;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractResponse implements Response {
    private long logId;
    private Object result;
    private Throwable exception;
    private RpcMethodInfo rpcMethodInfo;
    private RpcFuture rpcFuture;
    private Map<String, Object> kvAttachment;
    private ByteBuf binaryAttachment;
    private int compressType;
    private NSHead nsHead;
    private SPHead spHead;

    public void reset() {
        logId = -1;
        result = null;
        exception = null;
        rpcMethodInfo = null;
        rpcFuture = null;
        nsHead = null;
        kvAttachment = null;
        binaryAttachment = null;
        compressType = 0;
    }
}
