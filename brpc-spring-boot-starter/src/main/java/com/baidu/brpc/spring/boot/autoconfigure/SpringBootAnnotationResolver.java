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

import com.baidu.bjf.remoting.protobuf.utils.JDKCompilerHelper;
import com.baidu.bjf.remoting.protobuf.utils.compiler.Compiler;
import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.naming.NamingOptions;
import com.baidu.brpc.naming.NamingServiceFactory;
import com.baidu.brpc.spring.RpcProxyFactoryBean;
import com.baidu.brpc.spring.RpcServiceExporter;
import com.baidu.brpc.spring.annotation.AbstractAnnotationParserCallback;
import com.baidu.brpc.spring.annotation.NamingOption;
import com.baidu.brpc.spring.annotation.RpcAnnotationResolverListener;
import com.baidu.brpc.spring.annotation.RpcExporter;
import com.baidu.brpc.spring.annotation.RpcProxy;
import com.baidu.brpc.spring.boot.autoconfigure.config.BrpcConfig;
import com.baidu.brpc.spring.boot.autoconfigure.config.BrpcProperties;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Supports annotation resolver for {@link RpcProxy} and {@link RpcExporter} under springboot.
 *
 * @author huwenwei
 */
@Setter
@Getter
@Slf4j
public class SpringBootAnnotationResolver extends AbstractAnnotationParserCallback
        implements InitializingBean, PriorityOrdered {
    /**
     * properties from spring boot application.yml
     */
    private BrpcProperties brpcProperties;

    /**
     * The rpc clients.
     */
    private List<RpcProxyFactoryBean> rpcClients = new ArrayList<RpcProxyFactoryBean>();

    /**
     * The port mapping exporters.
     */
    private Map<Integer, RpcServiceExporter> portMappingExporters = new HashMap<Integer, RpcServiceExporter>();

    /**
     * The compiler.
     */
    private Compiler compiler;

    /**
     * status to control start only once.
     */
    private AtomicBoolean started = new AtomicBoolean(false);

    /* the default naming service url */
    private String namingServiceUrl;

    /**
     * The default registry center service for all service
     */
    private NamingServiceFactory namingServiceFactory;

    /**
     * The default interceptor for all service
     */
    private Interceptor interceptor;

    /**
     * The protobuf rpc annotation resolver listener.
     */
    private RpcAnnotationResolverListener protobufRpcAnnotationResolverListener;

    private int order = Ordered.LOWEST_PRECEDENCE - 3;

    @Override
    public Object annotationAtField(Annotation t, Object value, String beanName, PropertyValues pvs,
                                    DefaultListableBeanFactory beanFactory, Field field) throws BeansException {
        if (t instanceof RpcProxy) {
            try {
                log.info("Annotation 'BrpcProxy' on field '" + field.getName() + "' for target '" + beanName
                        + "' created");
                return parseRpcProxyAnnotation((RpcProxy) t, field.getType(), beanFactory);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        return value;
    }

    @Override
    public Object annotationAtMethod(Annotation t, Object bean, String beanName, PropertyValues pvs,
                                     DefaultListableBeanFactory beanFactory, Method method) throws BeansException {
        if (t instanceof RpcProxy) {
            try {
                log.info("Annotation 'BrpcProxy' on method '" + method.getName() + "' for target '" + beanName
                        + "' created");
                return parseRpcProxyAnnotation((RpcProxy) t, method.getParameterTypes()[0], beanFactory);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        return null;
    }

    @Override
    public Object annotationAtType(Annotation t, Object bean, String beanName,
                                   ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (t instanceof RpcExporter) {
            log.info("Annotation 'RpcExporter' for target '" + beanName + "' created");
            parseRpcExporterAnnotation((RpcExporter) t, beanFactory, beanFactory.getBean(beanName));
        }
        return bean;
    }

    @Override
    public void annotationAtTypeAfterStarted(Annotation t, Object bean, String beanName,
                                             ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (started.compareAndSet(false, true)) {
            // do export service here
            Collection<RpcServiceExporter> values = portMappingExporters.values();
            for (RpcServiceExporter rpcServiceExporter : values) {
                try {
                    rpcServiceExporter.afterPropertiesSet();
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public void destroy() throws Exception {
        if (rpcClients != null) {
            for (RpcProxyFactoryBean bean : rpcClients) {
                try {
                    bean.destroy();
                } catch (Exception e) {
                    log.error(e.getMessage(), e.getCause());
                }
            }
        }

        if (portMappingExporters != null) {
            Collection<RpcServiceExporter> exporters = portMappingExporters.values();
            for (RpcServiceExporter rpcServiceExporter : exporters) {
                try {
                    rpcServiceExporter.destroy();
                } catch (Exception e) {
                    log.error(e.getMessage(), e.getCause());
                }
            }
        }

        if (protobufRpcAnnotationResolverListener != null) {
            protobufRpcAnnotationResolverListener.destroy();
        }

    }

    @Override
    public Class<? extends Annotation> getTypeAnnotation() {
        return RpcExporter.class;
    }

    @Override
    public List<Class<? extends Annotation>> getMethodFieldAnnotation() {
        List<Class<? extends Annotation>> list = new ArrayList<Class<? extends Annotation>>();
        list.add(RpcProxy.class);
        return list;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (compiler != null) {
            JDKCompilerHelper.setCompiler(compiler);
        }
    }

    @Override
    public int getOrder() {
        return order;
    }

    /**
     * Sets the compiler.
     *
     * @param compiler the new compiler
     */
    public void setCompiler(Compiler compiler) {
        this.compiler = compiler;
    }

    /**
     * Parses the {@link RpcExporter} annotation.
     *
     * @param rpcExporter the rpc exporter
     * @param beanFactory the bean factory
     * @param bean        the bean
     */
    private void parseRpcExporterAnnotation(RpcExporter rpcExporter,
                                            ConfigurableListableBeanFactory beanFactory,
                                            Object bean) {
        Class<?> serviceClass = AopUtils.getTargetClass(bean);
        Class<?>[] interfaces = ClassUtils.getAllInterfacesForClass(serviceClass);
        if (interfaces.length != 1) {
            throw new RuntimeException("service interface num must equal 1, " + serviceClass.getName());
        }
        Class<?> serviceInterface = interfaces[0];
        BrpcConfig brpcConfig = getServiceConfig(beanFactory, serviceInterface);

        // if there are multi service on one port, the first service configs effect only.
        Integer port = brpcConfig.getServer().getPort();
        RpcServiceExporter rpcServiceExporter = portMappingExporters.get(port);
        if (rpcServiceExporter == null) {
            rpcServiceExporter = new RpcServiceExporter();
            portMappingExporters.put(port, rpcServiceExporter);
            rpcServiceExporter.setServicePort(port);
            rpcServiceExporter.copyFrom(brpcConfig.getServer());
            if (brpcConfig.getNaming() != null) {
                rpcServiceExporter.setNamingServiceUrl(brpcConfig.getNaming().getNamingServiceUrl());
            }
        }

        // interceptor
        if (brpcConfig.getServer() != null
                && StringUtils.isNoneBlank(brpcConfig.getServer().getInterceptorBeanName())) {
            Interceptor interceptor = beanFactory.getBean(
                    brpcConfig.getServer().getInterceptorBeanName(), Interceptor.class);
            if (rpcServiceExporter.getInterceptors() != null) {
                rpcServiceExporter.getInterceptors().add(interceptor);
            } else {
                rpcServiceExporter.setInterceptors(Arrays.asList(interceptor));
            }
        }

        // naming options
        rpcServiceExporter.getServiceNamingOptions().put(bean, brpcConfig.getNaming());

        if (brpcConfig.getServer() != null && brpcConfig.getServer().isUseSharedThreadPool()) {
            rpcServiceExporter.getCustomOptionsServiceMap().put(brpcConfig.getServer(), bean);
        } else {
            rpcServiceExporter.getRegisterServices().add(bean);
        }

        if (protobufRpcAnnotationResolverListener != null) {
            protobufRpcAnnotationResolverListener.onRpcExporterAnnotationParsered(
                    rpcExporter, port, bean, rpcServiceExporter.getRegisterServices());
        }
    }

    /**
     * Parses the rpc proxy annotation.
     *
     * @param rpcProxy    the rpc proxy
     * @param beanFactory the bean factory
     * @return the object
     * @throws Exception the exception
     */
    private Object parseRpcProxyAnnotation(RpcProxy rpcProxy,
                                           Class serviceInterface,
                                           DefaultListableBeanFactory beanFactory) throws Exception {
        RpcProxyFactoryBean rpcProxyFactoryBean;
        String factoryBeanName = "&" + serviceInterface.getSimpleName();
        try {
            rpcProxyFactoryBean = beanFactory.getBean(factoryBeanName, RpcProxyFactoryBean.class);
            if (rpcProxyFactoryBean != null) {
                return rpcProxyFactoryBean.getObject();
            }
        } catch (NoSuchBeanDefinitionException ex) {
            // continue the following logic to create new factory bean
        }

        rpcProxyFactoryBean = createRpcProxyFactoryBean(rpcProxy, beanFactory, serviceInterface);
        rpcClients.add(rpcProxyFactoryBean);
        Object object = rpcProxyFactoryBean.getObject();
        if (protobufRpcAnnotationResolverListener != null) {
            protobufRpcAnnotationResolverListener.onRpcProxyAnnotationParsed(rpcProxy, rpcProxyFactoryBean,
                    rpcProxyFactoryBean.getObject());
        }

        return object;
    }

    /**
     * Creates the rpc proxy factory bean.
     *
     * @return the rpc proxy factory bean
     */
    private RpcProxyFactoryBean createRpcProxyFactoryBean(RpcProxy rpcProxy,
                                                          DefaultListableBeanFactory beanFactory,
                                                          Class serviceInterface) {
        GenericBeanDefinition beanDef = new GenericBeanDefinition();
        beanDef.setBeanClass(RpcProxyFactoryBean.class);
        beanDef.setDependsOn("brpcApplicationContextUtils");
        MutablePropertyValues values = new MutablePropertyValues();
        BrpcConfig brpcConfig = getServiceConfig(beanFactory, serviceInterface);
        for (Field field : RpcClientOptions.class.getDeclaredFields()) {
            try {
                field.setAccessible(true);
                values.addPropertyValue(field.getName(), field.get(brpcConfig.getClient()));
            } catch (Exception ex) {
                log.warn("field not exist:", ex);
            }
        }
        values.addPropertyValue("serviceInterface", serviceInterface);
        values.addPropertyValue("serviceId", rpcProxy.name());
        if (brpcConfig.getNaming() != null) {
            values.addPropertyValue("namingServiceUrl", brpcConfig.getNaming().getNamingServiceUrl());
            values.addPropertyValue("group", brpcConfig.getNaming().getGroup());
            values.addPropertyValue("version", brpcConfig.getNaming().getVersion());
            values.addPropertyValue("ignoreFailOfNamingService",
                    brpcConfig.getNaming().isIgnoreFailOfNamingService());
        }

        // interceptor
        String interceptorName = brpcConfig.getClient().getInterceptorBeanName();
        if (!StringUtils.isBlank(interceptorName)) {
            Interceptor interceptor = beanFactory.getBean(interceptorName, Interceptor.class);
            values.addPropertyValue("interceptors", Arrays.asList(interceptor));
        }

        beanDef.setPropertyValues(values);
        String serviceInterfaceBeanName = serviceInterface.getSimpleName();
        beanFactory.registerBeanDefinition(serviceInterfaceBeanName, beanDef);
        return beanFactory.getBean("&" + serviceInterfaceBeanName, RpcProxyFactoryBean.class);
    }

    private BrpcConfig getServiceConfig(ListableBeanFactory beanFactory, Class<?> serviceInterface) {
        if (brpcProperties == null) {
            brpcProperties = beanFactory.getBean(BrpcProperties.class);
            if (brpcProperties == null) {
                throw new RuntimeException("bean of BrpcProperties is null");
            }
        }
        return brpcProperties.getServiceConfig(serviceInterface);
    }

}
