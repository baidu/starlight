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

import com.baidu.brpc.spring.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * register {@link CommonAnnotationBeanPostProcessor} with ImportBeanDefinitionRegistrar,
 * instead of AutoConfigure class, so that MergeableAnnotationBeanPostProcessor can be init before other beans.
 */
public class BeanPostProcessorRegister implements ImportBeanDefinitionRegistrar {
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry) {
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(MergeableAnnotationBeanPostProcessor.class);
        beanDefinition.setSynthetic(true);
        MutablePropertyValues values = new MutablePropertyValues();
        values.addPropertyValue("callback", new SpringBootAnnotationResolver());
        beanDefinition.setPropertyValues(values);
        registry.registerBeanDefinition("mergeableAnnotationBeanPostProcessor", beanDefinition);
    }
}
