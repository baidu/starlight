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

import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.rpc.RpcContext;
import com.baidu.cloud.starlight.springcloud.client.cluster.Cluster;
import com.baidu.cloud.starlight.springcloud.client.cluster.LoadBalancer;
import com.baidu.cloud.starlight.springcloud.client.cluster.RequestContext;
import com.baidu.cloud.starlight.springcloud.client.cluster.route.label.LabelClusterSelector;
import com.baidu.cloud.starlight.springcloud.client.cluster.route.label.LabelSelectorRouter;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightClientProperties;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightRouteProperties;
import com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by liuruisen on 2021/12/8.
 */
public class LabelSelectorRouterTest {

    @Test
    public void route() {
        StarlightRouteProperties routeProperties = new StarlightRouteProperties();
        routeProperties.setEnabled(true);
        StarlightRouteProperties.Selector selector = new StarlightRouteProperties.Selector();
        selector.setEnableProviderSelector(true);
        selector.setGlobalSelector("env=offline");
        selector.setProviderSelector(Collections.singletonMap("app-a", "EM_PLATFORM in (online,onlinenew)"));
        routeProperties.setLabelSelector(selector);
        LabelSelectorRouter router = new LabelSelectorRouter("app-a", routeProperties, new StarlightClientProperties(),
            Mockito.mock(LoadBalancer.class));

        RpcRequest request = new RpcRequest();
        request.setServiceName("com.baidu.TestService");
        request.setServiceName("echo");

        Cluster cluster = router.route(new RequestContext(request, RpcContext.getContext()));
        assertNotNull(cluster);
        assertTrue(cluster.getClusterSelector() instanceof LabelClusterSelector);
        String labelSelector = ((LabelClusterSelector) cluster.getClusterSelector()).getMeta()
            .get(SpringCloudConstants.LABEL_SELECTOR_ROUTE_KEY);
        assertTrue(labelSelector.contains("env=offline"));
        assertTrue(labelSelector.contains("EM_PLATFORM in (online,onlinenew)"));
    }
}