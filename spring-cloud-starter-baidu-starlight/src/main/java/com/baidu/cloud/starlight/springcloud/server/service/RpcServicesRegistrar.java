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
 
package com.baidu.cloud.starlight.springcloud.server.service;

import com.baidu.cloud.starlight.api.utils.StringUtils;
import com.baidu.cloud.starlight.springcloud.server.annotation.StarlightScan;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by liuruisen on 2019-05-20.
 */
public class RpcServicesRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private static final String STARLIGHT_SERVER_ENABLE_KEY = "starlight.server.enable";

    private static final String SERVER_ENABLE = "true";
    private Environment environment;

    // ImportBeanDefinitionRegistrar method
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

        String serverEnable = environment.getProperty(STARLIGHT_SERVER_ENABLE_KEY);
        if (!StringUtils.hasText(serverEnable) || !serverEnable.trim().equalsIgnoreCase(SERVER_ENABLE)) {
            return;
        }

        Set<String> packagesToScan = new HashSet<>();
        if (importingClassMetadata.hasAnnotation(StarlightScan.class.getName())) {
            Map<String, Object> selfBasePackages =
                importingClassMetadata.getAnnotationAttributes(StarlightScan.class.getName());
            if (selfBasePackages != null && selfBasePackages.size() > 0) {
                packagesToScan.addAll(Arrays.asList((String[]) selfBasePackages.get("basePackages")));
            }
        }

        packagesToScan.add(ClassUtils.getPackageName(importingClassMetadata.getClassName()));

        registerRpcServicePostProcessor(packagesToScan, registry);
    }

    /**
     * Register BeanDefinitionPostProcessor
     *
     * @param packageToScan
     * @param registry
     */
    private void registerRpcServicePostProcessor(Set<String> packageToScan, BeanDefinitionRegistry registry) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(RpcServicePostProcessor.class);
        builder.addConstructorArgValue(packageToScan);

        AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
        BeanDefinitionReaderUtils.registerWithGeneratedName(beanDefinition, registry);
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
