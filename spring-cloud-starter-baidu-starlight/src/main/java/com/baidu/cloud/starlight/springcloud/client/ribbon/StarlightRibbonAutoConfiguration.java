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

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.cloud.netflix.ribbon.RibbonEagerLoadProperties;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by liuruisen on 2020/12/3.
 */
@Configuration
@AutoConfigureAfter(name = {"com.baidu.cloud.starlight.springcloud.client.StarlightClientAutoConfiguration",
    "com.baidu.cloud.gravity.discovery.discovery.ribbon.RibbonGravityDiscoveryAutoConfiguration",
    "org.springframework.cloud.consul.discovery.RibbonConsulAutoConfiguration",
    "com.baidu.formula.consul.discovery.RibbonConsulAutoConfiguration"})
@RibbonClients(defaultConfiguration = StarlightRibbonConfiguration.class)
public class StarlightRibbonAutoConfiguration {

    @Bean
    @ConditionalOnProperty(value = "ribbon.eager-load.enabled")
    public RibbonNamedContextInitializer ribbonNamedContextInitializer(SpringClientFactory clientFactory,
        RibbonEagerLoadProperties properties) {
        return new RibbonNamedContextInitializer(clientFactory, properties);
    }
}
