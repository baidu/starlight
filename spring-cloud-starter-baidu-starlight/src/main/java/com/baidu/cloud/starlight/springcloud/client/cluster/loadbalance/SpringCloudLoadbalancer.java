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
 
package com.baidu.cloud.starlight.springcloud.client.cluster.loadbalance;

import com.baidu.cloud.starlight.api.exception.StarlightRpcException;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.rpc.RpcContext;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import com.baidu.cloud.starlight.core.rpc.SingleStarlightClient;
import com.baidu.cloud.starlight.springcloud.client.cluster.ClusterSelector;
import com.baidu.cloud.starlight.springcloud.client.cluster.LoadBalancer;
import com.baidu.cloud.starlight.springcloud.client.cluster.StarlightLBRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;

import static com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants.REQUEST_ROUTE_KEY;

/**
 * Springcloud implementation of {@link LoadBalancer},
 * use {@link LoadBalancerClient} to execute load balancing logic
 * Created by liuruisen on 2021/9/26.
 */
public class SpringCloudLoadbalancer implements LoadBalancer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringCloudLoadbalancer.class);

    private LoadBalancerClient loadBalancerClient;

    public SpringCloudLoadbalancer(LoadBalancerClient loadBalancerClient) {
        this.loadBalancerClient = loadBalancerClient;
    }

    @Override
    public ServiceInstance choose(ClusterSelector clusterSelector) {

        // 路由采用的方案为：
        // 通过RpcContext设置ClusterMeta信息，在StarlightLoadBalancer的choose方法中执行筛选
        // StarlightLoadBalancer举例，如ribbon的BaseLoadBalancer实现
        long chooseStart = System.currentTimeMillis();
        RpcContext.getContext().set(REQUEST_ROUTE_KEY, clusterSelector);
        ServiceInstance instance = loadBalancerClient.choose(clusterSelector.getServiceId());
        RpcContext.getContext().remove(REQUEST_ROUTE_KEY);
        LOGGER.debug("Spring cloud loadblancer choose instance for {} cost {}",
                clusterSelector.getServiceId(), System.currentTimeMillis() - chooseStart);
        return instance;
    }


    @Override
    public void execute(ClusterSelector clusterSelector, SingleStarlightClient starlightClient,
                        ServiceInstance instance,
                        Request request, RpcCallback callback) {
        try {
            loadBalancerClient.execute(clusterSelector.getServiceId(), instance,
                    new StarlightLBRequest(starlightClient, request, callback));
        } catch (Throwable e) {
            throw new StarlightRpcException("Failed to execute request in loadbalancer, "
                    + "instance " + instance.getHost() + ":" + instance.getPort(), e);
        }
    }

    /**
     * TODO: 可后续可实现动态更新负载均衡算法的逻辑
     * LoadBalancer的Spring cloud实现更新负载均衡规则时，
     * 不能直接替换LoadBalancer实现，需changeLoadBalanceRule方法在内部进行更改
     * 注意，考虑change时的并发保护
     * public void changeLoadBalanceRule(String ruleName) {
     *         // do nothing
     * }
     */

}
