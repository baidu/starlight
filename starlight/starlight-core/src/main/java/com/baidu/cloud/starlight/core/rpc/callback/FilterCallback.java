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
import com.baidu.cloud.starlight.api.filter.Filter;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.model.RpcResponse;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import io.netty.util.Timeout;

/**
 * Async Filter Callback 异步过滤链的Callback，包裹BizWrapCallback or FutureCallback Created by liuruisen on 2019/12/8.
 */
public class FilterCallback implements RpcCallback {

    private final Filter filter;

    private final RpcCallback callback; // BizWrapCallback or FutureCallback

    private final Request request;

    public FilterCallback(RpcCallback callback, Filter filter, Request request) {
        this.callback = callback;
        this.filter = filter;
        this.request = request;
    }

    @Override
    public void onResponse(Response response) {
        filter.filterResponse(response, request);
        if (callback != null) {
            callback.onResponse(response);
        }
    }

    @Override
    public void onError(Throwable e) {
        Response response = new RpcResponse(request.getId());
        response.setErrorMsg(e.getMessage());
        if (e instanceof RpcException) {
            response.setStatus(((RpcException) e).getCode());
        }
        filter.filterResponse(response, request);
        if (callback != null) {
            callback.onError(e);
        }
    }

    @Override
    public Request getRequest() {
        return this.request;
    }

    @Override
    public void addTimeout(Timeout timeout) {
        callback.addTimeout(timeout);
    }
}
