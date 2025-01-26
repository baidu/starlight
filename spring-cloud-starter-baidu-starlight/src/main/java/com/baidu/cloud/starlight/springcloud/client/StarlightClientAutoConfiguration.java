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
 
package com.baidu.cloud.starlight.springcloud.client;


import com.baidu.cloud.starlight.springcloud.client.annotation.RpcProxy;
import com.baidu.cloud.starlight.springcloud.client.cluster.LoadBalancer;
import com.baidu.cloud.starlight.springcloud.client.cluster.SingleStarlightClientManager;
import com.baidu.cloud.starlight.springcloud.client.cluster.loadbalance.SpringCloudLoadbalancer;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightClientProperties;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightRouteProperties;
import com.baidu.cloud.starlight.springcloud.common.ApplicationContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadFactory;

/**
 * Created by liuruisen on 2020/3/5.
 */
@Configuration
@ConditionalOnClass(RpcProxy.class)
@EnableConfigurationProperties(value = {StarlightClientProperties.class, StarlightRouteProperties.class})
public class StarlightClientAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(StarlightClientAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public RpcProxyAnnotationBeanPostProcessor rpcProxyBeanPostProcessor() {
        return new RpcProxyAnnotationBeanPostProcessor();
    }

    @Bean
    @ConditionalOnMissingBean
    public ApplicationContextUtils applicationContextUtils() {
        return new ApplicationContextUtils();
    }

    @Bean(destroyMethod = "destroyAll")
    @ConditionalOnMissingBean
    public SingleStarlightClientManager singleStarlightClientManager() {
        return SingleStarlightClientManager.getInstance();
    }

    @Bean
    @ConditionalOnMissingBean
    public RpcProxyProviders rpcProxyProviders(StarlightRouteProperties routeProperties) {
        RpcProxyProviders proxyProviders = new RpcProxyProviders();
        long startTime = System.currentTimeMillis();
        Reflections reflections = new Reflections("com", new FieldAnnotationsScanner());
        Set<Field> fields = reflections.getFieldsAnnotatedWith(RpcProxy.class);
        for (Field field : fields) {
            RpcProxy rpcProxy = field.getAnnotation(RpcProxy.class);
            if (!StringUtils.isEmpty(rpcProxy.name())
                    && StringUtils.isEmpty(rpcProxy.remoteUrl())) {
                if (routeProperties.getServiceIdMap() != null
                        && routeProperties.getServiceIdMap().containsKey(rpcProxy.name())) {
                    proxyProviders.getProviders().add(routeProperties.getServiceIdMap().get(rpcProxy.name()));
                } else {
                    proxyProviders.getProviders().add(rpcProxy.name());
                }
            }
        }
        LOGGER.info("Scanpackage {}, Provider list size is {}, content is {}, cost {}",
                "com", proxyProviders.getProviders().size(), proxyProviders, System.currentTimeMillis() - startTime);
        return proxyProviders;
    }

    @Bean
    @ConditionalOnMissingBean
    public LoadBalancer springCloudLoadBalancer(LoadBalancerClient loadBalancerClient) {
        SpringCloudLoadbalancer springCloudLoadbalancer = new SpringCloudLoadbalancer(loadBalancerClient);
        return springCloudLoadbalancer;
    }
}
