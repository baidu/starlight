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

import com.baidu.cloud.starlight.springcloud.client.StarlightClientAutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClientsProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerEagerLoadProperties;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClientSpecification;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.config.LoadBalancerAutoConfiguration;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;

/**
 * Created by liuruisen on 2020/12/3.
 */
@Configuration
@AutoConfigureBefore(value = {LoadBalancerAutoConfiguration.class})
@AutoConfigureAfter(value = {StarlightClientAutoConfiguration.class})
@LoadBalancerClients(defaultConfiguration = StarlightLoadbalancerConfiguration.class)
public class StarlightLoadBalancerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LoadBalancerClientFactory loadBalancerClientFactory(LoadBalancerClientsProperties properties,
        ObjectProvider<List<LoadBalancerClientSpecification>> configurations) {
        LoadBalancerClientFactory clientFactory = new StarlightLoadBalancerClientFactory(properties);
        clientFactory.setConfigurations(configurations.getIfAvailable(Collections::emptyList));
        return clientFactory;
    }

    @Bean
    @ConditionalOnBean(LoadBalancerClientFactory.class)
    @ConditionalOnMissingBean
    public LoadBalancerClient weightedBlockingLoadBalancerClient(LoadBalancerClientFactory loadBalancerClientFactory) {
        return new WeightedBlockLoadBalanceClient(loadBalancerClientFactory);
    }

    @Bean
    public LoadbalancerNamedContextInitializer allEagerContextInitializer(LoadBalancerClientFactory clientFactory,
        LoadBalancerEagerLoadProperties properties) {
        return new LoadbalancerNamedContextInitializer(clientFactory, properties);
    }
}
