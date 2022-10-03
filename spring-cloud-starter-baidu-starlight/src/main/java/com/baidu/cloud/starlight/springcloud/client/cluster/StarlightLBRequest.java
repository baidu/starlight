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
 
package com.baidu.cloud.starlight.springcloud.client.cluster;

import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.rpc.StarlightClient;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;

/**
 * Created by liuruisen on 2020/1/4.
 */
public class StarlightLBRequest implements LoadBalancerRequest<Response> {

    private StarlightClient starlightClient;

    private Request request;

    private RpcCallback callback;

    public StarlightLBRequest(StarlightClient client, Request request, RpcCallback callback) {
        this.starlightClient = client;
        this.request = request;
        this.callback = callback;
    }

    @Override
    public Response apply(ServiceInstance instance) throws Exception {
        starlightClient.request(request, callback);
        return null;
    }

    /**
     * get original rpc request
     * 
     * @return
     */
    public Request getDelegateRequest() {
        return request;
    }
}
