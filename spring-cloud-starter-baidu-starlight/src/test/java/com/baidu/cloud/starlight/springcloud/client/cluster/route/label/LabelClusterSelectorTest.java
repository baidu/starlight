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

import com.baidu.cloud.starlight.springcloud.client.cluster.route.label.LabelClusterSelector;
import com.baidu.cloud.starlight.springcloud.client.ribbon.StarlightRibbonServer;
import com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Created by liuruisen on 2021/12/6.
 */
public class LabelClusterSelectorTest {

    @Test
    public void filterEndpointsByLabel() {

        String labelSelector = "env=online&&EM_PLATFORM=onlinenew&&EM_LOGIC_IDC=bj";

        LabelClusterSelector labelService = new LabelClusterSelector();
        labelService.getMeta().put(SpringCloudConstants.LABEL_SELECTOR_ROUTE_KEY, labelSelector);

        long startTime10 = System.currentTimeMillis();
        List result1 = labelService.selectorClusterInstances(endpoints(10));
        System.out.println("LabelService filter 10 endpoint cost: " + (System.currentTimeMillis() - startTime10));
        assertEquals(0, result1.size());

        long startTime500 = System.currentTimeMillis();
        List result2 = labelService.selectorClusterInstances(endpoints(500));
        System.out.println("LabelService filter 500 endpoint cost: " + (System.currentTimeMillis() - startTime500));
        assertEquals(0, result2.size());

        long startTime1000 = System.currentTimeMillis();
        List result3 = labelService.selectorClusterInstances(endpoints(1000));
        System.out.println("LabelService filter 1000 endpoint cost: " + (System.currentTimeMillis() - startTime1000));
        assertEquals(0, result3.size());

        long startTime5000 = System.currentTimeMillis();
        List result4 = labelService.selectorClusterInstances(endpoints(5000));
        System.out.println("LabelService filter 5000 endpoint cost: " + (System.currentTimeMillis() - startTime5000));
        assertEquals(0, result4.size());

        long startTime10000 = System.currentTimeMillis();
        List result5 = labelService.selectorClusterInstances(endpoints(10000));
        System.out.println("LabelService filter 10000 endpoint cost: " + (System.currentTimeMillis() - startTime10000));
        assertEquals(0, result5.size());

    }

    @Test
    public void filterEndpointsByLabel2() {
        String labelSelector = "env=online&&EM_PLATFORM in (onlinenew,online)";

        LabelClusterSelector labelService = new LabelClusterSelector();
        labelService.getMeta().put(SpringCloudConstants.LABEL_SELECTOR_ROUTE_KEY, labelSelector);

        long startTime10 = System.currentTimeMillis();
        List result1 = labelService.selectorClusterInstances(endpoints(10));
        System.out.println("LabelService filter 10 endpoint cost: " + (System.currentTimeMillis() - startTime10));
        assertEquals(10, result1.size());

        long startTime500 = System.currentTimeMillis();
        List result2 = labelService.selectorClusterInstances(endpoints(500));
        System.out.println("LabelService filter 500 endpoint cost: " + (System.currentTimeMillis() - startTime500));
        assertEquals(500, result2.size());

        long startTime1000 = System.currentTimeMillis();
        List result3 = labelService.selectorClusterInstances(endpoints(1000));
        System.out.println("LabelService filter 1000 endpoint cost: " + (System.currentTimeMillis() - startTime1000));
        assertEquals(1000, result3.size());

        long startTime5000 = System.currentTimeMillis();
        List result4 = labelService.selectorClusterInstances(endpoints(5000));
        System.out.println("LabelService filter 5000 endpoint cost: " + (System.currentTimeMillis() - startTime5000));
        assertEquals(5000, result4.size());

        long startTime10000 = System.currentTimeMillis();
        List result5 = labelService.selectorClusterInstances(endpoints(10000));
        System.out.println("LabelService filter 10000 endpoint cost: " + (System.currentTimeMillis() - startTime10000));
        assertEquals(10000, result5.size());

    }

    private List<StarlightRibbonServer> endpoints(Integer size) {
        List<StarlightRibbonServer> endpoints = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            Map<String, String> labels = new HashMap<>();
            labels.put("EM_PRODUCT_LINE", "ns");
            labels.put("EM_APP", "service");
            labels.put("env", "online");
            if (i % 2 == 0) {
                labels.put("EM_PLATFORM", "online");
                labels.put("EM_LOGIC_IDC", "bj");
            } else {
                labels.put("EM_PLATFORM", "onlinenew");
                labels.put("EM_LOGIC_IDC", "nj");
            }

            StarlightRibbonServer starlightServer = new StarlightRibbonServer("ip." + i, i);
            starlightServer.setMetadata(labels);
            endpoints.add(starlightServer);
        }

        return endpoints;
    }

}
