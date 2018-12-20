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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.naming.NamingService;
import com.baidu.brpc.server.RpcServerOptions;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import com.baidu.bjf.remoting.protobuf.utils.JDKCompilerHelper;
import com.baidu.brpc.spring.RpcProxyFactoryBean;
import com.baidu.brpc.spring.RpcServiceExporter;
import com.baidu.bjf.remoting.protobuf.utils.compiler.Compiler;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;

/**
 * Supports annotation resolver for {@link RpcProxy} and {@link RpcExporter}.
 *
 * @author xiemalin
 * @since 2.17
 */
public class RpcAnnotationResolver extends AbstractAnnotationParserCallback implements InitializingBean {

    /** log this class. */
    protected static final Log LOGGER = LogFactory.getLog(RpcAnnotationResolver.class);

    /** The rpc clients. */
    private List<RpcProxyFactoryBean> rpcClients = new ArrayList<RpcProxyFactoryBean>();

    /** The port mapping expoters. */
    private Map<Integer, RpcServiceExporter> portMappingExpoters = new HashMap<Integer, RpcServiceExporter>();

    /** The compiler. */
    private Compiler compiler;

    /** status to control start only once. */
    private AtomicBoolean started = new AtomicBoolean(false);

    /** The registry center service. */
    private NamingService registryCenterService;

    /** The client interceptor. */
    private Interceptor clientInterceptor;

    /** The protobuf rpc annotation ressolver listener. */
    private RpcAnnotationResolverListener protobufRpcAnnotationRessolverListener;

    /** The server interceptor. */
    private Interceptor serverInterceptor;

    @Override
    public Object annotationAtType(Annotation t, Object bean, String beanName,
            ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (t instanceof RpcExporter) {
            LOGGER.info("Annotation 'RpcExporter' for target '" + beanName + "' created");

            // to fix AOP effective of target bean so instead of using
            // beanFactory.getBean(beanName)
            parseRpcExporterAnnotation((RpcExporter) t, beanFactory, beanFactory.getBean(beanName));
        }
        return bean;
    }

    /**
     * Parses the rpc exporter annotation.
     *
     * @param rpcExporter the rpc exporter
     * @param beanFactory the bean factory
     * @param bean the bean
     */
    private void parseRpcExporterAnnotation(RpcExporter rpcExporter, ConfigurableListableBeanFactory beanFactory,
            Object bean) {

        String port = parsePlaceholder(rpcExporter.port());
        // convert to integer and throw exception on error
        int intPort = Integer.valueOf(port);

        RpcServiceExporter rpcServiceExporter = portMappingExpoters.get(intPort);
        if (rpcServiceExporter == null) {
            rpcServiceExporter = new RpcServiceExporter();
            // get RpcClientOptions
            String rpcServerOptionsBeanName = parsePlaceholder(rpcExporter.rpcServerOptionsBeanName());

            RpcServerOptions rpcServerOptions;
            if (StringUtils.isBlank(rpcServerOptionsBeanName)) {
                rpcServerOptions = new RpcServerOptions();
            } else {
                // if not exist throw exception
                rpcServerOptions =
                        (RpcServerOptions) beanFactory.getBean(rpcServerOptionsBeanName, RpcServerOptions.class);
            }

            rpcServiceExporter = new RpcServiceExporter();
            String interceptorName = parsePlaceholder(rpcExporter.interceptorBeanName());
            if (!StringUtils.isBlank(interceptorName)) {
                Interceptor interceptor = beanFactory.getBean(interceptorName, Interceptor.class);
                List<Interceptor> interceptors = new ArrayList<Interceptor>();
                interceptors.add(interceptor);
                rpcServiceExporter.setInterceptors(interceptors);
            } else {
                if (serverInterceptor != null) {
                    List<Interceptor> interceptors = new ArrayList<Interceptor>();
                    interceptors.add(serverInterceptor);
                    rpcServiceExporter.setInterceptors(interceptors);
                }
            }

            rpcServiceExporter.setServicePort(intPort);
            try {
                BeanUtils.copyProperties(rpcServiceExporter, rpcServerOptions);
            } catch (Exception ex) {
                throw new RuntimeException("copy server options failed:", ex);
            }

            rpcServiceExporter.setRegistryCenterService(registryCenterService);
            portMappingExpoters.put(intPort, rpcServiceExporter);
        }

        // do register service
        List<Object> registerServices = rpcServiceExporter.getRegisterServices();
        if (registerServices == null) {
            registerServices = new ArrayList<Object>();
        }
        registerServices.add(bean);

        if (protobufRpcAnnotationRessolverListener != null) {
            protobufRpcAnnotationRessolverListener.onRpcExporterAnnotationParsered(rpcExporter, intPort, bean,
                    registerServices);
        }

        rpcServiceExporter.setRegisterServices(registerServices);
    }

    @Override
    public void annotationAtTypeAfterStarted(Annotation t, Object bean, String beanName,
            ConfigurableListableBeanFactory beanFactory) throws BeansException {

        if (started.compareAndSet(false, true)) {
            // do export service here
            Collection<RpcServiceExporter> values = portMappingExpoters.values();
            for (RpcServiceExporter rpcServiceExporter : values) {
                try {
                    rpcServiceExporter.afterPropertiesSet();
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.baidu.brpc.spring.annotation.AnnotationParserCallback#
     * annotationAtField(java.lang.annotation. Annotation, java.lang.Object, java.lang.String,
     * org.springframework.beans.PropertyValues,
     * org.springframework.beans.factory.config.ConfigurableListableBeanFactory, java.lang.reflect.Field)
     */
    @Override
    public Object annotationAtField(Annotation t, Object value, String beanName, PropertyValues pvs,
            DefaultListableBeanFactory beanFactory, Field field) throws BeansException {
        if (t instanceof RpcProxy) {
            try {
                LOGGER.info("Annotation 'BrpcProxy' on field '" + field.getName() + "' for target '" + beanName
                        + "' created");
                return parseRpcProxyAnnotation((RpcProxy) t, field.getType(), beanFactory);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        return value;
    }

    /**
     * Parses the rpc proxy annotation.
     *
     * @param rpcProxy the rpc proxy
     * @param beanFactory the bean factory
     * @return the object
     * @throws Exception the exception
     */
    private Object parseRpcProxyAnnotation(RpcProxy rpcProxy,
                                           Class serviceInterface,
                                           DefaultListableBeanFactory beanFactory)
            throws Exception {
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

        // get RpcClientOptions
        String rpcClientOptionsBeanName = parsePlaceholder(rpcProxy.rpcClientOptionsBeanName());
        RpcClientOptions rpcClientOptions;
        if (StringUtils.isBlank(rpcClientOptionsBeanName)) {
            rpcClientOptions = new RpcClientOptions();
        } else {
            // if not exist throw exception
            rpcClientOptions = beanFactory.getBean(rpcClientOptionsBeanName, RpcClientOptions.class);
        }

        String serviceUrl = parsePlaceholder(rpcProxy.serviceUrl());
        rpcProxyFactoryBean =
                createRpcProxyFactoryBean(rpcProxy, serviceInterface, beanFactory, rpcClientOptions, serviceUrl);

        rpcClients.add(rpcProxyFactoryBean);
        Object object = rpcProxyFactoryBean.getObject();
        if (protobufRpcAnnotationRessolverListener != null) {
            // TODO: why create new RpcProxyFactoryBean?
//            RpcProxyFactoryBean newRpcProxyFactoryBean =
//                    createRpcProxyFactoryBean(rpcProxy, beanFactory, rpcClientOptions, serviceUrl);
            protobufRpcAnnotationRessolverListener.onRpcProxyAnnotationParsed(rpcProxy, rpcProxyFactoryBean,
                    rpcProxyFactoryBean.getObject());
        }

        return object;
    }

    /**
     * Creates the rpc proxy factory bean.
     *
     * @param rpcProxy the rpc proxy
     * @param beanFactory the bean factory
     * @param rpcClientOptions the rpc client options
     * @param serviceUrl naming service url
     * @return the rpc proxy factory bean
     */
    protected RpcProxyFactoryBean createRpcProxyFactoryBean(RpcProxy rpcProxy,
                                                            Class serviceInterface,
                                                            DefaultListableBeanFactory beanFactory,
                                                            RpcClientOptions rpcClientOptions,
                                                            String serviceUrl) {
        GenericBeanDefinition beanDef = new GenericBeanDefinition();
        beanDef.setBeanClass(RpcProxyFactoryBean.class);
        MutablePropertyValues values = new MutablePropertyValues();
        for (Field field : rpcClientOptions.getClass().getFields()) {
            try {
                values.addPropertyValue(field.getName(), field.get(rpcClientOptions));
            } catch (Exception ex) {
                LOGGER.warn("field not exist:", ex);
            }
        }
        values.addPropertyValue("serviceInterface", serviceInterface);
        values.addPropertyValue("serviceUrl", serviceUrl);
        values.addPropertyValue("lookupStubOnStartup", rpcProxy.lookupStubOnStartup());
        String interceptorName = parsePlaceholder(rpcProxy.interceptorBeanName());
        if (!StringUtils.isBlank(interceptorName)) {
            Interceptor interceptor = beanFactory.getBean(interceptorName, Interceptor.class);
            List<Interceptor> interceptors = new ArrayList<Interceptor>();
            interceptors.add(interceptor);
            values.addPropertyValue("interceptors", interceptors);
        } else {
            if (clientInterceptor != null) {
                List<Interceptor> interceptors = new ArrayList<Interceptor>();
                interceptors.add(clientInterceptor);
                values.addPropertyValue("interceptors", interceptors);
            }
        }

        beanDef.setPropertyValues(values);
        String serviceInterfaceBeanName = serviceInterface.getSimpleName();
        beanFactory.registerBeanDefinition(serviceInterfaceBeanName, beanDef);
        return beanFactory.getBean("&" + serviceInterfaceBeanName, RpcProxyFactoryBean.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.baidu.jprotobuf.pbrpc.spring.annotation.AnnotationParserCallback#
     * annotationAtMethod(java.lang.annotation. Annotation, java.lang.Object, java.lang.String,
     * org.springframework.beans.PropertyValues,
     * org.springframework.beans.factory.config.DefaultListableBeanFactory, java.lang.reflect.Method)
     */
    @Override
    public Object annotationAtMethod(Annotation t, Object bean, String beanName, PropertyValues pvs,
            DefaultListableBeanFactory beanFactory, Method method) throws BeansException {
        if (t instanceof RpcProxy) {
            try {
                LOGGER.info("Annotation 'BrpcProxy' on method '" + method.getName() + "' for target '" + beanName
                        + "' created");
                return parseRpcProxyAnnotation((RpcProxy) t, method.getParameterTypes()[0], beanFactory);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.baidu.brpc.spring.annotation.AnnotationParserCallback# getTypeAnnotation()
     */
    @Override
    public Class<? extends Annotation> getTypeAnnotation() {
        return RpcExporter.class;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.baidu.brpc.spring.annotation.AnnotationParserCallback# getMethodFieldAnnotation()
     */
    @Override
    public List<Class<? extends Annotation>> getMethodFieldAnnotation() {
        List<Class<? extends Annotation>> list = new ArrayList<Class<? extends Annotation>>();
        list.add(RpcProxy.class);
        return list;
    }

    /*
     * @see com.baidu.brpc.spring.annotation.AnnotationParserCallback
     */
    @Override
    public void destroy() throws Exception {
        if (rpcClients != null) {
            for (RpcProxyFactoryBean bean : rpcClients) {
                try {
                    bean.destroy();
                } catch (Exception e) {
                    LOGGER.fatal(e.getMessage(), e.getCause());
                }
            }
        }

        if (portMappingExpoters != null) {
            Collection<RpcServiceExporter> exporters = portMappingExpoters.values();
            for (RpcServiceExporter rpcServiceExporter : exporters) {
                try {
                    rpcServiceExporter.destroy();
                } catch (Exception e) {
                    LOGGER.fatal(e.getMessage(), e.getCause());
                }
            }
        }

        if (protobufRpcAnnotationRessolverListener != null) {
            protobufRpcAnnotationRessolverListener.destroy();
        }

    }

    /**
     * Sets the compiler.
     *
     * @param compiler the new compiler
     */
    public void setCompiler(Compiler compiler) {
        this.compiler = compiler;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        if (compiler != null) {
            JDKCompilerHelper.setCompiler(compiler);
        }

    }

}
