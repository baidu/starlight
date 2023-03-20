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

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.exception.StarlightRpcException;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.rpc.callback.Callback;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import io.netty.util.Timeout;

/**
 * Wrap Biz Callback, Async Call: Callback mode 用户采用Callback的异步调用方式时使用此类(依据用户参数信息进行判断) 在StarlightClient request 前封装
 * Created by liuruisen on 2019/12/8.
 */
public class BizWrapCallback implements RpcCallback {

    private final Callback bizCallback;

    private final Request request;

    private Timeout timeout;

    public BizWrapCallback(Callback bizCallback, Request request) {
        this.bizCallback = bizCallback;
        this.request = request;
    }

    @Override
    public void onResponse(Response response) {
        cancelTimeout();
        if (Constants.SUCCESS_CODE.equals(response.getStatus())) {
            bizCallback.onResponse(response.getResult());
        } else {
            Throwable exception = null;
            if (response.getException() != null) {
                exception = response.getException();
            } else {
                exception =
                    new StarlightRpcException(response.getStatus(), response.getErrorMsg(), response.getException());
            }
            bizCallback.onError(exception);
        }
    }

    @Override
    public void onError(Throwable e) {
        cancelTimeout();
        bizCallback.onError(e);
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
