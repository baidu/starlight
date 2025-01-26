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

import com.baidu.cloud.starlight.springcloud.common.InstanceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClientsProperties;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.AnnotationConfigRegistry;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.Assert;

public class StarlightLoadBalancerClientFactory extends LoadBalancerClientFactory {

    private static final String GRAVITY_DISCOVERY_CLIENT =
        "com.baidu.cloud.gravity.discovery.discovery.GravityDiscoveryClient";

    private static final String BNS_DISCOVERY_CLIENT = "com.baidu.cloud.bns.discovery.BnsDiscoveryClient";

    @Autowired
    private LoadBalancerClientsProperties properties;

    public StarlightLoadBalancerClientFactory(LoadBalancerClientsProperties properties) {
        super(properties);
    }

    @Override
    public void registerBeans(String name, GenericApplicationContext context) {

        Assert.isInstanceOf(AnnotationConfigRegistry.class, context);
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) context;

        if (InstanceUtils.isGravityServiceId(name)) {
            // 注册gravity服务发现

            BeanDefinitionBuilder beanDefinitionBuilder =
                BeanDefinitionBuilder.genericBeanDefinition(GRAVITY_DISCOVERY_CLIENT);
            GenericBeanDefinition beanDefinition = new GenericBeanDefinition(beanDefinitionBuilder.getBeanDefinition());
            beanDefinition.setPrimary(true);

            registry.registerBeanDefinition("delegate", beanDefinition);
        } else if (InstanceUtils.isBnsServiceId(name)) {
            // 注册bns服务发现
            BeanDefinitionBuilder beanDefinitionBuilder =
                BeanDefinitionBuilder.genericBeanDefinition(BNS_DISCOVERY_CLIENT);
            GenericBeanDefinition beanDefinition = new GenericBeanDefinition(beanDefinitionBuilder.getBeanDefinition());
            beanDefinition.setPrimary(true);

            registry.registerBeanDefinition("delegate", beanDefinition);

        }

        super.registerBeans(name, context);
    }

}
