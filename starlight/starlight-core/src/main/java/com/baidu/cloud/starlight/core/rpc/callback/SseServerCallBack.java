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
 
package com.baidu.cloud.starlight.core.rpc.callback;

import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.transport.channel.RpcChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server端 sse服务调用callback
 */
public class SseServerCallBack extends BaseInvokeCallBack {
    private static Logger logger = LoggerFactory.getLogger(SseServerCallBack.class);

    public SseServerCallBack(Request request, RpcChannel context) {
        super(request, context);
    }

    private volatile boolean isErrorExecuted = false;
    private volatile boolean isResponseExecuted = false;

    private Throwable throwable;

    @Override
    public synchronized void onResponse(Response response) {
        if (!isErrorExecuted) {
            isResponseExecuted = true;
            super.onResponse(response);
        } else {
            // 错误响应已经返回了
            logger.error("Error occurred while method invoke.");
            throw new IllegalStateException("Error occurred while method invoke.", throwable);
        }
    }

    @Override
    public synchronized void onError(Throwable e) {
        if (!isResponseExecuted && !isErrorExecuted) {
            isErrorExecuted = true;
            throwable = e;
            super.onError(e);
        } else {
            // 已经正常响应了
            logger.warn("Response occurred while method invoke.");
            throw new IllegalStateException("Response occurred while method invoke.", throwable);
        }
    }
}
