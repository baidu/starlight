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
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import com.baidu.cloud.starlight.core.rpc.SingleStarlightClient;
import org.springframework.cloud.client.ServiceInstance;

/**
 * Starlight LoadBalancer interface 实现类可为Ribbon，也可为springcloudloadbalancer，或者统一为springcloudLoadBalancer Created by
 * liuruisen on 2021/9/6.
 */
public interface LoadBalancer {

    /**
     * Choose service instance
     *
     * @param clusterSelector
     * @return
     */
    ServiceInstance choose(ClusterSelector clusterSelector);

    /**
     * Choose service instance and execute request
     * 重要原因是为兼容SpringCloud的LoadbalancerClient的execute方法，其execute方法中会含各种执行逻辑如Ribbon的统计信息 可按照需求考虑不兼容: 原因与替换方案
     * Ribbon执行逻辑为统计请求信息，存储RibbonContext；SpringCloudLoadBalancer为在执行前后切面；
     * Ribbon的统计信息使用starlight自研方式；springcloudloadbalancer切面能力可使用或定制集群级filter
     *
     * @param clusterSelector
     * @param instance
     * @param request
     * @param callback
     */
    void execute(ClusterSelector clusterSelector, SingleStarlightClient starlightClient, ServiceInstance instance,
        Request request, RpcCallback callback);

}
