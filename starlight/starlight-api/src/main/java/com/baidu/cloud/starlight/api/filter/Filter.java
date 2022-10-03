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
 
package com.baidu.cloud.starlight.api.filter;

import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.rpc.Invoker;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;

/**
 * Async Filter Created by liuruisen on 2019/12/5.
 */
public interface Filter {
    /**
     * Filter Request
     * 
     * @param invoker
     * @param request
     * @param callback
     */
    void filterRequest(Invoker invoker, Request request, RpcCallback callback);

    /**
     * Filter async response
     * 
     * @param response
     * @param request
     */
    void filterResponse(Response response, Request request);

}
