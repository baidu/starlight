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
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;

import java.util.Map;

/**
 * Cluster and subset cluster manager 管理集群的相关行为，如集群lb策略、outlier、超时 Created by liuruisen on 2021/9/6.
 */
public interface Cluster {

    /**
     * Cluster具有执行业务请求的定位，其管控的StarlightClient应refer 服务接口
     * 
     * @param referServices
     */
    void setServiceRefers(Map<Class<?>, ServiceConfig> referServices);

    /**
     * Dynamically update cluster's config NOTE: must be thread safe
     */
    default void updateConfig(Object clusterConfig) {
        // TODO 开放集群管理能力后支持
        throw new UnsupportedOperationException("Update config is not support currently");
    }

    /**
     * Every cluster holds one {@link LoadBalancer}
     * 
     * @return
     */
    LoadBalancer getLoadBalancer();

    /**
     * Metadata of the cluster
     * 
     * @return
     */
    ClusterSelector getClusterSelector();

    /**
     * Executing requests in sub-clusters, the sub-cluster can execute its own special logic， such as outlier
     *
     * @param request
     * @param callback
     */
    void execute(Request request, RpcCallback callback);
}
