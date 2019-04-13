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

import java.util.Map;

import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.client.RpcFuture;
import com.baidu.brpc.protocol.nshead.NSHead;

import io.netty.buffer.ByteBuf;

public interface Response {
    Object getResult();

    void setResult(Object result);

    long getLogId();

    void setLogId(long logId);

    Throwable getException();

    NSHead getNsHead();

    void setNsHead(NSHead nsHead);

    void setException(Throwable exception);

    RpcMethodInfo getRpcMethodInfo();

    void setRpcMethodInfo(RpcMethodInfo rpcMethodInfo);

    RpcFuture getRpcFuture();

    void setRpcFuture(RpcFuture rpcFuture);

    Map<String, String> getKvAttachment();

    void setKvAttachment(Map<String, String> kvAttachment);

    ByteBuf getBinaryAttachment();

    void setBinaryAttachment(ByteBuf binaryAttachment);

    int getCompressType();

    void setCompressType(int compressType);

    void reset();
}
