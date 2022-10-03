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

import com.baidu.cloud.starlight.api.rpc.RpcContext;
import com.baidu.cloud.starlight.springcloud.client.cluster.ClusterSelector;
import com.baidu.cloud.starlight.springcloud.client.cluster.route.label.match.LabelParser;
import com.baidu.cloud.starlight.springcloud.client.cluster.route.label.match.LabelSelector;
import com.baidu.cloud.starlight.springcloud.client.cluster.route.label.match.LabelSelectorRequirement;
import com.baidu.cloud.starlight.springcloud.client.ribbon.StarlightRibbonServer;
import com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants;
import com.baidu.cloud.thirdparty.apache.commons.lang3.StringUtils;
import com.netflix.loadbalancer.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Migrate from gravity and extend ClusterSelector
 */
public class LabelClusterSelector extends ClusterSelector<StarlightRibbonServer> {

    private static final Logger LOG = LoggerFactory.getLogger(LabelClusterSelector.class);

    private LabelParser labelParser = new LabelParser();

    @Override
    public List<StarlightRibbonServer> selectorClusterInstances(List<StarlightRibbonServer> originList) {
        // 支持海若请求级的label selector选择, 用完删除防止向下传递（ugly实现）
        String labelSelector = RpcContext.getContext().get(SpringCloudConstants.REQUEST_LABEL_SELECTOR_ROUTE_KEY);
        RpcContext.getContext().remove(SpringCloudConstants.REQUEST_LABEL_SELECTOR_ROUTE_KEY);
        if (originList == null || originList.isEmpty()) {
            return originList;
        }

        if (StringUtils.isEmpty(labelSelector)) {
            labelSelector = getMeta().get(SpringCloudConstants.LABEL_SELECTOR_ROUTE_KEY);
        }
        if (StringUtils.isEmpty(labelSelector)) {
            return originList;
        }

        List<StarlightRibbonServer> result = originList;
        try {
            LabelSelector selector = labelParser.parse(labelSelector);
            if (selector.getMatchExpressions() != null) {
                result = originList.stream().filter(server -> matchLabels(server, selector.getMatchExpressions()))
                    .collect(Collectors.toList());
                recordSelectorResult(labelSelector, originList, result);
            }
        } catch (Throwable e) {
            LOG.error("[LABEL_ROUTE]LabelClusterSelector#selectorClusterInstances failed, service: {}, label: {}",
                getServiceId(), labelSelector, e);
        }

        if (result == null || result.isEmpty()) {
            recordSelectorEmptyResult(labelSelector, originList);
        }

        return result;
    }

    private boolean matchLabels(StarlightRibbonServer server, List<LabelSelectorRequirement> expressions) {
        for (LabelSelectorRequirement expression : expressions) {
            if (!matchLabel(server, expression)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchLabel(StarlightRibbonServer server, LabelSelectorRequirement expression) {
        if ("namespace".equals(expression.getKey())) {
            // 直接返回true, gravity服务发现时会携带namespace信息，保证发现的服务为指定ns下的
            // NOTICE: 仅适用gravity场景
            return true;
        }
        Map<String, String> meta = server.getMetadata();
        String value = meta != null ? meta.get(expression.getKey()) : null;
        return expression.labelValueMatch(value);
    }

    private void recordSelectorEmptyResult(String labelSelector, List<StarlightRibbonServer> origin) {
        LOG.info("[LABEL_ROUTE]The filtered servers is empty, serviceId:{}, label:{}, originServers:{}", getServiceId(),
            labelSelector, origin);
    }

    private void recordSelectorResult(String labelSelector, List<StarlightRibbonServer> origin,
        List<StarlightRibbonServer> result) {
        LOG.debug("[LABEL_ROUTE]Filter servers of service: {} by label: {}, servers: {}/{}, {}", getServiceId(),
            labelSelector, origin.size(), result.size(),
            result.stream().map(Server::getId).collect(Collectors.joining(",")));
    }
}
