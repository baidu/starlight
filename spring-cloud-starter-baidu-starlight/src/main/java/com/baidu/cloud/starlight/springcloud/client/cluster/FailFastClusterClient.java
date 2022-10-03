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
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightClientProperties;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightRouteProperties;
import com.baidu.cloud.starlight.springcloud.configuration.Configuration;
import com.baidu.cloud.thirdparty.netty.util.Timeout;
import org.springframework.cloud.client.discovery.DiscoveryClient;

/**
 * FailFastClusterClient SPI name: "failfast" Created by liuruisen on 2020/2/27.
 */
public class FailFastClusterClient extends AbstractClusterClient {

    public FailFastClusterClient(String name, StarlightClientProperties properties, LoadBalancer loadBalancer,
        DiscoveryClient discoveryClient, SingleStarlightClientManager clientManager, Configuration configuration,
        StarlightRouteProperties routeProperties) {
        super(name, properties, loadBalancer, discoveryClient, clientManager, configuration, routeProperties);
    }

    @Override
    public void request(Request request, RpcCallback callback) {
        super.request(request, new FailFastClusterCallback(callback));
    }

    protected class FailFastClusterCallback implements RpcCallback {
        private final RpcCallback chainedCallback;

        public FailFastClusterCallback(RpcCallback callback) {
            chainedCallback = callback;
        }

        @Override
        public void addTimeout(Timeout timeout) {
            chainedCallback.addTimeout(timeout);
        }

        @Override
        public Request getRequest() {
            return chainedCallback.getRequest();
        }

        @Override
        public void onResponse(Response response) {
            chainedCallback.onResponse(response);
        }

        @Override
        public void onError(Throwable e) {
            chainedCallback.onError(e);
        }
    }

}
