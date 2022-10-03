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
import com.baidu.cloud.starlight.springcloud.client.cluster.ClusterSelector;
import com.baidu.cloud.starlight.springcloud.client.cluster.SingleStarlightClientManager;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightRouteProperties;
import com.baidu.cloud.starlight.springcloud.client.ribbon.StarlightRibbonServer;
import com.baidu.cloud.starlight.springcloud.client.ribbon.StarlightServerListFilter;
import com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants;
import com.baidu.cloud.thirdparty.netty.util.Timeout;
import com.netflix.loadbalancer.Server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants.ROUTE_SERVER_LIST_FILTER_ORDER;

/**
 * Created by liuruisen on 2021/11/3.
 */
public class RoutableServerListFilter implements StarlightServerListFilter<Server> {

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
    public List<Server> getFilteredList(List<Server> servers) {

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
        // 类型不对，则返回，防止误判
        if (!(servers.get(0) instanceof StarlightRibbonServer)) {
            return servers;
        }

        ClusterSelector clusterSelector = (ClusterSelector) obj;

        List<Server> result = new ArrayList<>(servers);
        try {
            result = clusterSelector.selectorClusterInstances(result);
        } catch (Throwable e) {
            LOGGER.error("Route select instances for serviceId {} failed, clusterSelector {}.", name,
                clusterSelector.getClass().getSimpleName(), e);
            return servers;
        }
        // 支持海若请求级的label selector选择, 用完删除防止向下传递（ugly实现）
        RpcContext.getContext().remove(SpringCloudConstants.REQUEST_LABEL_SELECTOR_ROUTE_KEY);

        return result;
    }

    @Override
    public Map<String, Timeout> getServerListFilterTasks() {
        return null;
    }

    @Override
    public void submitTimerTask(Server server, Integer execDelay) {
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
