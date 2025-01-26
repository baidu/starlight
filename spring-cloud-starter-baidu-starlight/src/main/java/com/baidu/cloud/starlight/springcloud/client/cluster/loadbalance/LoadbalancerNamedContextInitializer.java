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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.cloud.client.loadbalancer.LoadBalancerEagerLoadProperties;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;

import java.util.HashSet;
import java.util.Set;

/**
 * 用于预加载Loadbalancer子Context以及相应的bean，优化第一次请求的速度，相应的会造成启动时间变长 Created by liuruisen on 2021/9/27.
 */
public class LoadbalancerNamedContextInitializer
    implements ApplicationListener<ApplicationStartedEvent>, ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadbalancerNamedContextInitializer.class);

    private static final String ALL_CLIENT_NAMES = "*";

    private final LoadBalancerClientFactory factory;

    private final LoadBalancerEagerLoadProperties eagerProperties;

    public LoadbalancerNamedContextInitializer(LoadBalancerClientFactory factory,
        LoadBalancerEagerLoadProperties properties) {
        this.factory = factory;
        this.eagerProperties = properties;
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        autoInit();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

    }

    /**
     * 自动初始化Context
     */
    public void autoInit() {
        if (eagerProperties.getClients() == null || eagerProperties.getClients().isEmpty()) {
            return;
        }
        try {
            String clientName0 = eagerProperties.getClients().get(0);
            Set<String> serviceNames = new HashSet<>(eagerProperties.getClients());
            // all clients init: *
            if (clientName0.trim().equalsIgnoreCase(ALL_CLIENT_NAMES)) {
                serviceNames = factory.getContextNames();

            }
            for (String clientName : serviceNames) {
                factory.getInstance(clientName);
            }
        } catch (Throwable e) {
            LOGGER.warn("Auto initialize loadbalancer contexts failed, caused by ", e);
        }
    }

    /**
     * 主动调用初始化context
     */
    public void manualInit(String clientName) {
        if (factory == null) {
            LOGGER.warn("SpringClientFactory is null when manual call init, plz make sure the bean is injection");
        }
        try {
            LOGGER.debug("Manual initialize loadbalancer context {} initializing", clientName);
            factory.getInstance(clientName);
            LOGGER.debug("Manual initialize loadbalancer context {} initialized", clientName);
        } catch (Throwable e) {
            LOGGER.warn("Manual initialize loadbalancer context " + clientName + " failed, caused by ", e);
        }
    }

}
