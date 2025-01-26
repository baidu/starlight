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

import com.baidu.cloud.starlight.springcloud.client.cluster.SingleStarlightClientManager;
import com.baidu.cloud.starlight.springcloud.client.cluster.route.RoutableServerListFilter;
import com.baidu.cloud.starlight.springcloud.client.outlier.OutlierEjectServerListFilter;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightClientProperties;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightRouteProperties;
import com.baidu.cloud.starlight.springcloud.client.shutdown.ShutdownServerListFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Created by liuruisen on 2020/12/3.
 */
@Configuration
public class StarlightLoadbalancerConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OutlierEjectServerListFilter outlierEjectServerListFilter(SingleStarlightClientManager clientManager,
        StarlightClientProperties clientProperties, Environment environment, LoadBalancerClientFactory clientFactory) {
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new OutlierEjectServerListFilter(clientManager, clientProperties, name);
    }

    @Bean
    @ConditionalOnMissingBean
    public ShutdownServerListFilter shutdownServerListFilter(SingleStarlightClientManager clientManager,
        StarlightClientProperties clientProperties) {
        return new ShutdownServerListFilter(clientManager, clientProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public RoutableServerListFilter routableServerListFilter(Environment environment,
        LoadBalancerClientFactory clientFactory, SingleStarlightClientManager clientManager,
        StarlightRouteProperties routeProperties) {
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new RoutableServerListFilter(clientManager, routeProperties, name);
    }

    @Bean
    @ConditionalOnBean(DiscoveryClient.class)
    @ConditionalOnMissingBean
    public ServiceInstanceListSupplier
        discoveryClientServiceInstanceListSupplier(ConfigurableApplicationContext context) {
        return ServiceInstanceListSupplier.builder().withBlockingDiscoveryClient()
            .with((context1, delegate) -> new StarlightActiveServiceInstanceListSupplier(context, delegate))
            .build(context);
    }
}
