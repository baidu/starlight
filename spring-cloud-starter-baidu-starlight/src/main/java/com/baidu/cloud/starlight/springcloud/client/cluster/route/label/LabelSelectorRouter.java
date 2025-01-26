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
 
package com.baidu.cloud.starlight.springcloud.client.cluster.route.label;

import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.utils.StringUtils;
import com.baidu.cloud.starlight.springcloud.client.cluster.Cluster;
import com.baidu.cloud.starlight.springcloud.client.cluster.ClusterSelector;
import com.baidu.cloud.starlight.springcloud.client.cluster.LoadBalancer;
import com.baidu.cloud.starlight.springcloud.client.cluster.route.AbstractRouter;
import com.baidu.cloud.starlight.springcloud.client.cluster.subcluster.DefaultCluster;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightClientProperties;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightRouteProperties;
import com.baidu.cloud.starlight.springcloud.common.RouteUtils;
import com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by liuruisen on 2021/12/7.
 */
public class LabelSelectorRouter extends AbstractRouter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LabelSelectorRouter.class);

    private static final Integer PRIORITY = Integer.MAX_VALUE - 1;

    private final String serviceId;

    private final LoadBalancer loadBalancer;

    private final StarlightRouteProperties routeProperties;

    private final StarlightClientProperties clientProperties;

    public LabelSelectorRouter(String serviceId,
                               StarlightRouteProperties routeProperties,
                               StarlightClientProperties clientProperties,
                               LoadBalancer loadBalancer) {
        this.serviceId = serviceId;
        this.routeProperties = routeProperties;
        this.clientProperties = clientProperties;
        this.loadBalancer = loadBalancer;
    }

    @Override
    public Cluster route(Request request) {
        long routeStart = System.currentTimeMillis();
        // TODO 后续可考虑用RDS支持label路由
        // 支持海若请求级的label selector选择, 用完删除防止向下传递（ugly实现）
        Map<String, Object> routeContext =
                (Map<String, Object>) request.getNoneAdditionKv().get(SpringCloudConstants.ROUTE_CONTEXT_KEY);
        LOGGER.debug("LabelSelectorRouter route context {}", routeContext);
        String labelSelector =
                (String) routeContext.get(SpringCloudConstants.REQUEST_LABEL_SELECTOR_ROUTE_KEY);
        LOGGER.debug("LabelSelectorRouter label selector from route context {}", labelSelector);
        if (StringUtils.isEmpty(labelSelector)) {
            labelSelector = routeProperties.getServiceLabelSelector(getRouteServiceId());
        }
        if (StringUtils.isEmpty(labelSelector)) {
            LOGGER.debug("[LABEL_ROUTE]LabelSelector for service {} is empty", getServiceId());
            labelSelector = "";
        }

        ClusterSelector clusterSelector = new LabelClusterSelector();
        clusterSelector.setServiceId(getServiceId());
        clusterSelector.setClusterName(getServiceId());

        Map<String, String> clusterLabels = new HashMap<>();
        clusterLabels.put(SpringCloudConstants.LABEL_SELECTOR_ROUTE_KEY, labelSelector);
        clusterSelector.setMeta(clusterLabels);

        recordRouteMatch(request, labelSelector, routeStart);

        return new DefaultCluster(clusterSelector, clientProperties, loadBalancer);
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public String getServiceId() {
        return this.serviceId;
    }

    @Override
    public String getRouteServiceId() {
        // 配置了map映射，使用map指定的value
        if (routeProperties.getServiceIdMap() != null
                && routeProperties.getServiceIdMap().containsKey(serviceId)) {
            return routeProperties.getServiceIdMap().get(serviceId);
        }
        return this.serviceId;
    }

    private void recordRouteMatch(Request request, String labelSelector, long routeStart) {
        LOGGER.info("[LABEL_ROUTE] Request matched label-selector route: "
                        + "serviceId {}, req{}, labelSelector {}, cost {}",
                getServiceId(), RouteUtils.reqMsg(request), labelSelector,
                System.currentTimeMillis() - routeStart);
    }
}
