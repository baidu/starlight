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

import java.util.HashMap;
import java.util.Map;

import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.client.RpcFuture;

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
    private Map<String, String> kvAttachment = new HashMap<String, String>();
    private ByteBuf binaryAttachment;
    private int compressType;

    public void reset() {
        logId = -1;
        result = null;
        exception = null;
        rpcMethodInfo = null;
        rpcFuture = null;
        kvAttachment.clear();
        delRefCntForServer();
        compressType = 0;
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
