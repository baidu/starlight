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
 
package com.baidu.cloud.starlight.springcloud.client.ribbon;

import com.baidu.cloud.starlight.springcloud.client.cluster.SingleStarlightClientManager;
import com.baidu.cloud.starlight.springcloud.client.cluster.route.RoutableServerListFilter;
import com.baidu.cloud.starlight.springcloud.client.outlier.OutlierEjectServerListFilter;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightClientProperties;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightRouteProperties;
import com.baidu.cloud.starlight.springcloud.client.shutdown.ShutdownServerListFilter;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.IPing;
import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.netflix.loadbalancer.ServerListFilter;
import com.netflix.loadbalancer.ServerListUpdater;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Created by liuruisen on 2020/12/3.
 */
@Configuration
public class StarlightRibbonConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OutlierEjectServerListFilter outlierEjectServerListFilter(IClientConfig config,
        SingleStarlightClientManager clientManager, StarlightClientProperties clientProperties) {
        return new OutlierEjectServerListFilter(clientManager, clientProperties, config.getClientName());
    }

    @Bean
    @ConditionalOnMissingBean
    public ShutdownServerListFilter shutdownServerListFilter(SingleStarlightClientManager clientManager,
        StarlightClientProperties clientProperties) {
        return new ShutdownServerListFilter(clientManager, clientProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public RoutableServerListFilter routableServerListFilter(IClientConfig config,
        SingleStarlightClientManager clientManager, StarlightRouteProperties routeProperties) {
        return new RoutableServerListFilter(clientManager, routeProperties, config.getClientName());
    }

    @Bean(destroyMethod = "destroy")
    @ConditionalOnMissingBean
    public StarlightActiveLoadBalancer starlightActiveLoadBalancer(SingleStarlightClientManager clientManager,
        IClientConfig config, ServerList<Server> serverList, ServerListFilter<Server> serverListFilter, IRule rule,
        IPing ping, ServerListUpdater serverListUpdater, List<StarlightServerListFilter> starlightFilters,
        StarlightClientProperties clientProperties) {
        StarlightActiveLoadBalancer starlightActiveLoadBalancer = new StarlightActiveLoadBalancer(clientManager, config,
            rule, ping, serverList, serverListFilter, serverListUpdater, clientProperties);
        starlightActiveLoadBalancer.setStarlightServerListFilters(starlightFilters);

        return starlightActiveLoadBalancer;
    }
}
