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
package com.baidu.brpc.spring.boot.autoconfigure;

import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.naming.NamingServiceFactory;
import com.baidu.brpc.spring.RpcProxyFactoryBean;
import com.baidu.brpc.spring.boot.autoconfigure.config.BrpcConfig;
import com.baidu.brpc.spring.boot.autoconfigure.config.BrpcProperties;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;

@Setter
@Getter
public class ProxyFactoryBean extends RpcProxyFactoryBean implements BeanFactoryAware {
    private ListableBeanFactory beanFactory;

    @Autowired
    private BrpcProperties properties;

    @Override
    public void afterPropertiesSet() throws Exception {
        BrpcConfig brpcConfig = getServiceConfig(getServiceInterface());
        this.copyFrom(brpcConfig.getClient());
        this.setNamingServiceUrl(brpcConfig.getNaming().getNamingServiceUrl());
        String namingServiceFactoryClassName = brpcConfig.getNaming().getNamingServiceFactory();
        try {
            NamingServiceFactory namingServiceFactory = (NamingServiceFactory)
                    Class.forName(namingServiceFactoryClassName).newInstance();
            this.setNamingServiceFactory(namingServiceFactory);
        } catch (Exception ex) {
            throw new RuntimeException("initialize naming factory failed", ex);
        }
        this.setGroup(brpcConfig.getNaming().getGroup());
        this.setVersion(brpcConfig.getNaming().getVersion());
        this.setIgnoreFailOfNamingService(brpcConfig.getNaming().isIgnoreFailOfNamingService());

        // interceptor
        if (brpcConfig.getClient() != null
                && StringUtils.isNoneBlank(brpcConfig.getClient().getInterceptorBeanName())) {
            Interceptor interceptor = beanFactory.getBean(
                    brpcConfig.getServer().getInterceptorBeanName(), Interceptor.class);
            this.setInterceptors(Arrays.asList(interceptor));
        }

        super.afterPropertiesSet();
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (ListableBeanFactory) beanFactory;
    }

    private BrpcConfig getServiceConfig(Class<?> serviceInterface) {
        BrpcConfig brpcConfig;
        if (properties.getCustom() != null) {
            brpcConfig = properties.getCustom().get(serviceInterface.getName());
            if (brpcConfig != null) {
                return brpcConfig;
            }
        }
        brpcConfig = properties.getGlobal();
        return brpcConfig;
    }
}
