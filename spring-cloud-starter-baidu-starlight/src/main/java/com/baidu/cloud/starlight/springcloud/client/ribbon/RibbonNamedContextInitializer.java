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

import com.baidu.cloud.starlight.springcloud.client.cluster.AbstractClusterClient;
import com.netflix.loadbalancer.ILoadBalancer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.cloud.netflix.ribbon.RibbonEagerLoadProperties;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;

import java.util.Map;

/**
 * 用于预加载Ribbon的子Context以及相应的bean {@link ILoadBalancer} Created by liuruisen on 2021/9/27.
 */
public class RibbonNamedContextInitializer
    implements ApplicationListener<ApplicationStartedEvent>, ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(RibbonNamedContextInitializer.class);

    private static final String ALL_CLIENT_NAMES = "*";

    private ApplicationContext applicationContext;

    private RibbonEagerLoadProperties eagerLoadProperties;

    private SpringClientFactory clientFactory;

    public RibbonNamedContextInitializer(SpringClientFactory clientFactory, RibbonEagerLoadProperties properties) {
        this.clientFactory = clientFactory;
        this.eagerLoadProperties = properties;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        autoInit();
    }

    /**
     * 自动初始化Context
     */
    public void autoInit() {
        if (eagerLoadProperties.getClients() == null) {
            return;
        }
        try {
            String clientName0 = eagerLoadProperties.getClients().get(0);
            // all clients init: *
            if (clientName0.trim().equalsIgnoreCase(ALL_CLIENT_NAMES)) {
                try {
                    Map<String, AbstractClusterClient> clusterClients =
                        applicationContext.getBeansOfType(AbstractClusterClient.class);
                    if (clusterClients != null) {
                        for (AbstractClusterClient clusterClient : clusterClients.values()) {
                            clientFactory.getInstance(clusterClient.getName(), ILoadBalancer.class);
                        }
                    }
                } catch (BeansException e) {
                    LOGGER.warn("Get AbstractClusterClient from applicationContext failed, caused by ", e);
                }
            } else {
                for (String clientName : eagerLoadProperties.getClients()) {
                    clientFactory.getInstance(clientName, ILoadBalancer.class);
                }
            }
        } catch (Throwable e) {
            LOGGER.warn("Auto initialize ribbon contexts failed, caused by ", e);
        }
    }

    /**
     * 主动调用初始化context
     */
    public void manualInit(String clientName) {
        if (clientFactory == null) {
            LOGGER.warn("SpringClientFactory is null when manual call init, plz make sure the bean is injection");
        }
        try {
            LOGGER.debug("Manual initialize ribbon context {} initializing", clientName);
            clientFactory.getInstance(clientName, ILoadBalancer.class);
            LOGGER.debug("Manual initialize ribbon context {} initialized", clientName);
        } catch (Throwable e) {
            LOGGER.warn("Manual initialize ribbon context " + clientName + " failed, caused by ", e);
        }
    }

}
