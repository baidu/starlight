/*
 * Copyright (c) 2018 Baidu, Inc. All Rights Reserved.
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
package com.baidu.brpc.spring;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.util.ReflectionUtils;

/**
 * Utility class for {@link PropertyPlaceholderConfigurer}.
 *
 * @author xiemalin
 * @since 2.17
 */
public final class PropertyPlaceholderConfigurerTool {

    /**
     * get {@link Properties} instance from  {@link ConfigurableListableBeanFactory}.
     *
     * @param beanFactory spring container
     * @return {@link Properties} instance
     */
    public static Properties getRegisteredPropertyResourceConfigurer(
            ConfigurableListableBeanFactory beanFactory) {
        Class clazz = PropertyPlaceholderConfigurer.class;
        Map beans = beanFactory.getBeansOfType(clazz);
        if (beans == null || beans.isEmpty()) {
            return null;
        }
        
        Object config = ((Map.Entry)beans.entrySet().iterator().next()).getValue();
        if (clazz.isAssignableFrom(config.getClass())) {
            Method m = ReflectionUtils.findMethod(clazz, "mergeProperties", new Class[0]);
            if (m != null) {
                m.setAccessible(true);
                return (Properties) ReflectionUtils.invokeMethod(m, config);
            }
        }
        return null;
    }
    
    /**
     * To create placeholder parser.
     *
     * @param propertyResource {@link Properties} instance
     * @return {@link PlaceholderResolver} instance
     */
    public static PlaceholderResolver createPlaceholderParser(
            final Properties propertyResource) {
        if (propertyResource == null) {
            return null;
        }
        PlaceholderResolver resolver = new PlaceholderResolver(
                new PlaceholderResolved() {
                    public String doResolved(String placeholder) {
                        return propertyResource.getProperty(placeholder);
                    }
                });
        return resolver;
    }
}
