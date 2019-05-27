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

import com.baidu.brpc.spring.annotation.RpcExporter;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class RpcExporterRegister extends AbstractRegister implements ImportBeanDefinitionRegistrar {
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry) {
        Map<Class, String> serviceExporterMap = new HashMap<>();
        AnnotationBeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator();
        Collection<BeanDefinition> candidates = getCandidates(resourceLoader);
        for (BeanDefinition candidate : candidates) {
            Class<?> clazz = getClass(candidate.getBeanClassName());
            Class<?>[] interfaces = ClassUtils.getAllInterfacesForClass(clazz);
            if (interfaces.length != 1) {
                throw new BeanInitializationException("bean interface num must equal 1, " + clazz.getName());
            }
            String serviceBeanName = beanNameGenerator.generateBeanName(candidate, registry);
            String old = serviceExporterMap.putIfAbsent(interfaces[0], serviceBeanName);
            if (old != null) {
                throw new RuntimeException("interface already be exported by bean name:" + old);
            }
            registry.registerBeanDefinition(serviceBeanName, candidate);
        }
    }

    private Collection<BeanDefinition> getCandidates(ResourceLoader resourceLoader) {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false, environment);

        scanner.addIncludeFilter(new AnnotationTypeFilter(RpcExporter.class));
        scanner.setResourceLoader(resourceLoader);
        return AutoConfigurationPackages.get(beanFactory).stream()
                .flatMap(basePackage -> scanner.findCandidateComponents(basePackage).stream())
                .collect(Collectors.toSet());
    }
}
