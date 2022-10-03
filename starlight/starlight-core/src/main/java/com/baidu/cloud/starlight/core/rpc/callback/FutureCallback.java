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

import com.baidu.cloud.starlight.api.exception.RpcException;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.model.ResultFuture;
import com.baidu.cloud.starlight.api.model.RpcResponse;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import com.baidu.cloud.thirdparty.netty.util.Timeout;

/**
 * Wrap Future Callback, Async Call: Future mode 用户采用Future的异步调用方式时使用此类(依据用户参数信息进行判断) 在StarlightClient request 前封装
 * Created by liuruisen on 2019/12/8.
 */
public class FutureCallback implements RpcCallback {
    private final ResultFuture resultFuture;

    private final Request request;

    private Timeout timeout;

    public FutureCallback(ResultFuture resultFuture, Request request) {
        this.resultFuture = resultFuture;
        this.request = request;
    }

    @Override
    public void onResponse(Response response) {
        cancelTimeout();
        resultFuture.putResponse(response);
    }

    @Override
    public void onError(Throwable e) {
        cancelTimeout();
        Response response = new RpcResponse(request.getId());
        response.setErrorMsg(e.getMessage());
        response.setProtocolName(request.getProtocolName());
        if (e instanceof RpcException) {
            response.setStatus(((RpcException) e).getCode());
        }
        resultFuture.putResponse(response);
    }

    @Override
    public Request getRequest() {
        return this.request;
    }

    @Override
    public void addTimeout(Timeout timeout) {
        this.timeout = timeout;
    }

    private void cancelTimeout() {
        if (timeout != null && !timeout.isCancelled()) {
            timeout.cancel();
        }
    }
}
