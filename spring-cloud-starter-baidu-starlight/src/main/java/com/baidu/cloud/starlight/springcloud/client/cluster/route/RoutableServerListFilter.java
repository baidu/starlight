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
 
package com.baidu.cloud.starlight.springcloud.client.cluster.route;

import com.baidu.cloud.starlight.api.rpc.RpcContext;
import com.baidu.cloud.starlight.api.utils.StringUtils;
import com.baidu.cloud.starlight.springcloud.client.cluster.ClusterSelector;
import com.baidu.cloud.starlight.springcloud.client.cluster.SingleStarlightClientManager;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightRouteProperties;
import com.baidu.cloud.starlight.springcloud.client.cluster.loadbalance.StarlightServerListFilter;
import com.baidu.cloud.starlight.springcloud.common.InstanceUtils;
import com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants;
import com.baidu.cloud.thirdparty.netty.util.Timeout;
import org.springframework.cloud.client.ServiceInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants.ROUTE_SERVER_LIST_FILTER_ORDER;

/**
 * Created by liuruisen on 2021/11/3.
 */
public class RoutableServerListFilter implements StarlightServerListFilter {

    private final SingleStarlightClientManager clientManager;

    private final StarlightRouteProperties routeProperties;

    private final String name;

    public RoutableServerListFilter(SingleStarlightClientManager clientManager,
        StarlightRouteProperties routeProperties, String name) {
        this.clientManager = clientManager;
        this.routeProperties = routeProperties;
        this.name = name;
    }

    @Override
    public List<ServiceInstance> getFilteredList(List<ServiceInstance> servers) {

        if (!routeProperties.getEnabled()) {
            return servers;
        }

        if (servers == null || servers.size() == 0) {
            return servers;
        }

        Object obj = RpcContext.getContext().get(SpringCloudConstants.REQUEST_ROUTE_KEY);
        if (!(obj instanceof ClusterSelector)) {
            return servers;
        }

        ClusterSelector clusterSelector = (ClusterSelector) obj;

        List<ServiceInstance> result = new ArrayList<>(servers);
        try {
            result = clusterSelector.selectorClusterInstances(result);
        } catch (Throwable e) {
            LOGGER.error("Route select instances for serviceId {} failed, clusterSelector {}.", name,
                clusterSelector.getClass().getSimpleName(), e);
            return servers;
        }
        // 支持海若请求级的label selector选择, 用完删除防止向下传递（ugly实现）
        RpcContext.getContext().remove(SpringCloudConstants.REQUEST_LABEL_SELECTOR_ROUTE_KEY);

        if ((result == null || result.isEmpty()) && InstanceUtils.isBnsServiceId(servers.get(0).getServiceId())) {
            // bns 服务兜底策略，因为BNS不是标准的Gravity服务但支持了内容路由，需要进行过滤；
            // 但不是所有的bns服务都配置了路由策略
            LOGGER.info("Service {} is bns type, route result is empty will return all", name);
            return servers;
        }

        return result;
    }

    @Override
    public Map<String, Timeout> getServerListFilterTasks() {
        return null;
    }

    @Override
    public void submitTimerTask(ServiceInstance server, Integer execDelay) {
        // do nothing
    }

    @Override
    public void destroy() {
        // do nothing
    }

    @Override
    public SingleStarlightClientManager getSingleClientManager() {
        return clientManager;
    }

    @Override
    public int getOrder() {
        return ROUTE_SERVER_LIST_FILTER_ORDER;
    }

}
