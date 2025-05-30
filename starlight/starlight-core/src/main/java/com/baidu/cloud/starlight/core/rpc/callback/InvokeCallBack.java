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

/**
 * Server端 服务调用callback
 */
public class InvokeCallBack extends BaseInvokeCallBack {

    public InvokeCallBack(Request request, RpcChannel context) {
        super(request, context);
    }

    private volatile boolean isExecuted = false;

    @Override
    public void onResponse(Response response) { // service execute return object
        if (!isExecuted) {
            isExecuted = true;
            super.onResponse(response);
        }
    }

    @Override
    public void onError(Throwable e) { // service execute throw exception
        if (!isExecuted) {
            isExecuted = true;
            super.onError(e);
        }
    }

}
