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
package com.baidu.brpc.spring.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import com.baidu.brpc.spring.PlaceholderResolver;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * Annotation parser call back interface.
 *
 * @author xiemalin
 * @see org.springframework.context.annotation.CommonAnnotationBeanPostProcessor
 */
public interface AnnotationParserCallback {

    /**
     * process all annotation on class type.
     * 
     * @param t
     *            annotation instance.
     * @param bean
     *            target bean
     * @param beanName
     *            target bean name
     * @param beanFactory
     *            spring bean factory
     * @return wrapped bean
     * @throws BeansException
     *             exceptions on spring beans create error.
     */
    Object annotationAtType(Annotation t, Object bean, String beanName,
                            ConfigurableListableBeanFactory beanFactory) throws BeansException;

    /**
     * process all annotation on class type after spring containter started.
     *
     * @param t            annotation instance.
     * @param bean            target bean
     * @param beanName            target bean name
     * @param beanFactory            spring bean factory
     * @throws BeansException             exceptions on spring beans create error.
     */
    void annotationAtTypeAfterStarted(Annotation t, Object bean,
                                      String beanName, ConfigurableListableBeanFactory beanFactory)
        throws BeansException;

    /**
     * process all annotation on class field.
     * 
     * @param t
     *            annotation instance.
     * @param value
     *            field value of target target
     * @param beanName
     *            target bean name
     * @param pvs
     *            bean property values
     * @param beanFactory
     *            spring bean factory
     * @param field
     *            field instance
     * @return field value
     * @throws BeansException
     *             exceptions on spring beans create error.
     */
    Object annotationAtField(Annotation t, Object value, String beanName,
                             PropertyValues pvs, DefaultListableBeanFactory beanFactory,
                             Field field) throws BeansException;

    /**
     * process all annotation on class method.
     * 
     * @param t
     *            annotation instance.
     * @param bean
     *            target bean
     * @param beanName
     *            target bean name
     * @param pvs
     *            bean property values
     * @param beanFactory
     *            spring bean factory
     * @param method
     *            method instance
     * @return method invoke parameter
     * @throws BeansException
     *             exceptions on spring beans create error.
     */
    Object annotationAtMethod(Annotation t, Object bean, String beanName,
                              PropertyValues pvs, DefaultListableBeanFactory beanFactory,
                              Method method) throws BeansException;

    /**
     * Gets the type annotation.
     *
     * @return the type annotation
     */
    Class<? extends Annotation> getTypeAnnotation();

    /**
     * Gets the method field annotation.
     *
     * @return the method field annotation
     */
    List<Class<? extends Annotation>> getMethodFieldAnnotation();

    /**
     * do destroy action on spring container close.
     * 
     * @throws Exception
     *             throw any exception
     */
    void destroy() throws Exception;

    /**
     * Sets the placeholder resolver.
     *
     * @param resolver the new placeholder resolver
     */
    void setPlaceholderResolver(PlaceholderResolver resolver);

}
