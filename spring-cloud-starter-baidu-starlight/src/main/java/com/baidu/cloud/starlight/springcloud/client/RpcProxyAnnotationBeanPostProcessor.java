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
 
package com.baidu.cloud.starlight.springcloud.client;

import com.baidu.cloud.starlight.api.rpc.StarlightClient;
import com.baidu.cloud.starlight.api.utils.EnvUtils;
import com.baidu.cloud.starlight.core.rpc.SingleStarlightClient;
import com.baidu.cloud.starlight.springcloud.client.annotation.RpcProxy;
import com.baidu.cloud.starlight.springcloud.client.cluster.FailFastClusterClient;
import com.baidu.cloud.starlight.springcloud.client.cluster.FailOverClusterClient;
import com.baidu.cloud.starlight.springcloud.client.cluster.LoadBalancer;
import com.baidu.cloud.starlight.springcloud.client.cluster.SingleStarlightClientManager;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightClientProperties;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightRouteProperties;
import com.baidu.cloud.starlight.springcloud.configuration.Configuration;
import com.baidu.cloud.thirdparty.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.Environment;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

/**
 * Created by liuruisen on 2019-06-26. TODO 研究AOT的逻辑，继承BeanRegistrationAotProcessor实现AOT逻辑 -
 * postProcessMergedBeanDefinition + postProcessProperties + findInjectionMetadata 参考 @autowire
 */
public class RpcProxyAnnotationBeanPostProcessor
    implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware, EnvironmentAware, PriorityOrdered {

    public static final String BEAN_NAME = "rpcClientAnnotationBeanPostProcessor";

    private static final String CLIENT_BEAN_NAME_SUFFIX = ".StarlightClient";

    /**
     * Bean name prefix for target beans behind scoped proxies. Used to exclude those targets from handler method
     * detection, in favor of the corresponding proxies.
     * <p>
     * see org.springframework.web.servlet.handler.AbstractHandlerMethodMapping
     */
    private static final String TARGET_NAME_PREFIX = "scopedTarget.";

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcProxyAnnotationBeanPostProcessor.class);

    private DefaultListableBeanFactory beanFactory;

    private PropertySourcesPlaceholdersResolver placeholdersResolver;

    @Override
    public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName)
        throws BeansException {
        Class<?> clazz = bean.getClass();
        List<Field> fields = FieldUtils.getAllFieldsList(clazz);
        if (fields != null && fields.size() > 0) {
            for (Field field : fields) {
                RpcProxy rpcProxy = field.getAnnotation(RpcProxy.class);
                if (rpcProxy != null) {
                    RpcProxyInfo rpcProxyInfo = resolveRpcProxy(rpcProxy); // get and set real @RpcProxy#name value
                    try {
                        Object proxyBean = buildRpcProxyBean(field.getType(), rpcProxyInfo);
                        ReflectionUtils.makeAccessible(field);
                        field.set(bean, proxyBean);
                    } catch (Exception e) {
                        throw new BeanCreationException(beanName, e);
                    }
                }
            }
        }
        return pvs;
    }

    /**
     * Resolve rpcProxy to get the real name
     *
     * @param rpcProxy
     * @return
     */
    private RpcProxyInfo resolveRpcProxy(RpcProxy rpcProxy) {
        String name = rpcProxy.name();
        if (!StringUtils.hasText(name)) {
            return new RpcProxyInfo(rpcProxy, name);
        }

        String realName = (String) placeholdersResolver.resolvePlaceholders(name);
        if (!StringUtils.hasText(realName)) {
            throw new IllegalArgumentException(
                "Resolve name value of @RpcProxy#name failed, " + "the value must not be null");
        }

        return new RpcProxyInfo(rpcProxy, realName);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 5; // 保证本BeanPostProcessor在Autowire PostProcessor之前执行
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if (beanFactory instanceof ConfigurableListableBeanFactory) {
            this.beanFactory = (DefaultListableBeanFactory) beanFactory;
        } else {
            throw new IllegalArgumentException(
                "RpcProxyAnnotationBeanPostProcessor requires a ConfigurableListableBeanFactory");
        }
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.placeholdersResolver = new PropertySourcesPlaceholdersResolver(environment);
    }

    private Object buildRpcProxyBean(Class<?> rpcProxyClass, RpcProxyInfo rpcProxyInfo) throws Exception {
        // get from spring ioc (cache)
        try {
            String proxyBeanName = "&" + proxyBeanName(rpcProxyInfo, rpcProxyClass);
            RpcProxyFactoryBean proxyBean = beanFactory.getBean(proxyBeanName, RpcProxyFactoryBean.class);
            if (proxyBean != null) {
                return proxyBean.getObject();
            }
        } catch (NoSuchBeanDefinitionException ex) {
            LOGGER.info("Get Bean {} form BeanFactory failed, will create", ex.getBeanName());
        }

        return rpcProxyBean(rpcProxyClass, rpcProxyInfo);
    }

    /**
     * Register {@link RpcProxyFactoryBean} and get proxy object
     *
     * @param proxyType
     * @param rpcProxyInfo
     * @return
     * @throws Exception
     */
    private Object rpcProxyBean(Class<?> proxyType, RpcProxyInfo rpcProxyInfo) throws Exception {
        // register or get starlight client
        StarlightClient starlightClient = registerOrGetStarlightClient(rpcProxyInfo);

        // register factory bean
        BeanDefinitionBuilder definitionBuilder =
            BeanDefinitionBuilder.genericBeanDefinition(RpcProxyFactoryBean.class);
        definitionBuilder.addPropertyValue("annotationInfos", rpcProxyInfo.getRpcProxy());
        definitionBuilder.addPropertyValue("clientName", rpcProxyInfo.getRealName());
        definitionBuilder.addPropertyValue("type", proxyType);
        definitionBuilder.addPropertyValue("client", starlightClient);
        definitionBuilder.addPropertyValue("clientProperties", clientProperties());
        definitionBuilder.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);

        AbstractBeanDefinition beanDefinition = definitionBuilder.getBeanDefinition();

        String proxyBeanName = proxyBeanName(rpcProxyInfo, proxyType);
        String aliasName = proxyBeanName;
        if (StringUtils.hasText(rpcProxyInfo.getRpcProxy().qualifier())) {
            aliasName = rpcProxyInfo.getRpcProxy().qualifier();
        }
        BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, proxyBeanName, new String[] {aliasName});
        BeanDefinitionReaderUtils.registerBeanDefinition(holder, beanFactory);

        RpcProxyFactoryBean proxyBean = beanFactory.getBean("&" + proxyBeanName, RpcProxyFactoryBean.class);
        return proxyBean.getObject();
    }

    /**
     * Register StarlightClient Bean Types: Single / Cluster
     *
     * @param rpcProxyInfo
     * @return
     */
    private StarlightClient registerOrGetStarlightClient(RpcProxyInfo rpcProxyInfo) {
        String clientBeanName = clientBeanName(rpcProxyInfo);

        RpcProxy rpcProxy = rpcProxyInfo.getRpcProxy();

        if (StringUtils.hasText(rpcProxy.remoteUrl())) {
            String protocol = null;
            if (rpcProxy.remoteUrl().contains("://")) {
                protocol = rpcProxy.remoteUrl().split("://")[0];
            }

            if (!StringUtils.hasText(rpcProxy.protocol()) && !StringUtils.hasText(protocol)) {
                throw new IllegalArgumentException(
                    "protocol in @RpcProxy#protocol is null, " + "when specify @RpcProxy#remoteUrl");
            }
            return registerOrGetSingleClient(clientBeanName, rpcProxyInfo);
        }

        if (StringUtils.hasText(rpcProxyInfo.getRealName())) {
            return registerOrGetClusterClient(clientBeanName, rpcProxyInfo);
        }

        return null;
    }

    /**
     * Support direct URL request
     *
     * @param clientBeanName
     * @param rpcProxyInfo
     * @return
     */
    private StarlightClient registerOrGetSingleClient(String clientBeanName, RpcProxyInfo rpcProxyInfo) {
        StarlightClient starlightClient = getClientFormIocCache(clientBeanName);
        if (starlightClient != null) {
            return starlightClient;
        }
        String remoteUrl = rpcProxyInfo.getRpcProxy().remoteUrl();
        if (remoteUrl.contains("://")) {
            remoteUrl = remoteUrl.split("://")[1];
        }
        String[] ipAndPort = remoteUrl.split(":");
        StarlightClientProperties properties = clientProperties();
        BeanDefinitionBuilder definitionBuilder =
            BeanDefinitionBuilder.genericBeanDefinition(SingleStarlightClient.class);
        definitionBuilder.addConstructorArgValue(ipAndPort[0]);
        definitionBuilder.addConstructorArgValue(ipAndPort[1]);
        definitionBuilder.addConstructorArgValue(properties.transportConfig(remoteUrl));
        definitionBuilder.setInitMethodName("init");
        definitionBuilder.setDestroyMethodName("destroy");
        definitionBuilder.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);

        AbstractBeanDefinition beanDefinition = definitionBuilder.getBeanDefinition();
        BeanDefinitionHolder holder =
            new BeanDefinitionHolder(beanDefinition, clientBeanName, new String[] {clientBeanName});
        // register
        BeanDefinitionReaderUtils.registerBeanDefinition(holder, beanFactory);
        // get bean
        return beanFactory.getBean(clientBeanName, StarlightClient.class);
    }

    /**
     * Support Cluster request
     *
     * @param clientBeanName
     * @param rpcProxyInfo
     * @return
     */
    private StarlightClient registerOrGetClusterClient(String clientBeanName, RpcProxyInfo rpcProxyInfo) {
        StarlightClient starlightClient = getClientFormIocCache(clientBeanName);
        if (starlightClient != null) {
            return starlightClient;
        }
        StarlightClientProperties properties = clientProperties();
        String clusterModel = clusterModel(properties, rpcProxyInfo);

        BeanDefinitionBuilder definitionBuilder = null;
        switch (clusterModel) {
            case "failfast":
                definitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(FailFastClusterClient.class);
                break;
            case "failover":
                definitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(FailOverClusterClient.class);
                break;
            default:
                throw new IllegalStateException(
                    "starlight.client.config {clusterModel} is null, " + "please config it before run");
        }
        LoadBalancer loadBalancer = beanFactory.getBean(LoadBalancer.class);
        DiscoveryClient discoveryClient = beanFactory.getBean(DiscoveryClient.class);
        SingleStarlightClientManager clientManager = beanFactory.getBean(SingleStarlightClientManager.class);
        Configuration configuration = null;
        try {
            configuration = beanFactory.getBean(Configuration.class);
        } catch (BeansException e) {
            LOGGER.error("No such bean of Configuration, does not depend on gravity?", e);
        }
        StarlightRouteProperties routeProperties = beanFactory.getBean(StarlightRouteProperties.class);
        definitionBuilder.addConstructorArgValue(rpcProxyInfo.getRealName());
        definitionBuilder.addConstructorArgValue(properties);
        definitionBuilder.addConstructorArgValue(loadBalancer);
        definitionBuilder.addConstructorArgValue(discoveryClient);
        definitionBuilder.addConstructorArgValue(clientManager);
        definitionBuilder.addConstructorArgValue(configuration);
        definitionBuilder.addConstructorArgValue(routeProperties);
        definitionBuilder.setInitMethodName("init");
        definitionBuilder.setDestroyMethodName("destroy");
        definitionBuilder.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);

        AbstractBeanDefinition beanDefinition = definitionBuilder.getBeanDefinition();
        BeanDefinitionHolder holder =
            new BeanDefinitionHolder(beanDefinition, clientBeanName, new String[] {clientBeanName});
        BeanDefinitionReaderUtils.registerBeanDefinition(holder, beanFactory);

        return beanFactory.getBean(clientBeanName, StarlightClient.class);
    }

    private StarlightClientProperties clientProperties() {
        StarlightClientProperties properties = beanFactory.getBean(StarlightClientProperties.class);
        if (properties.getConfig() == null || properties.getConfig().size() == 0) {
            throw new IllegalStateException("starlight.client.config is null, please config it before run");
        }

        return properties;
    }

    /**
     * Get Cluster model from properties
     *
     * @param properties
     * @param rpcProxyInfo
     * @return
     */
    private String clusterModel(StarlightClientProperties properties, RpcProxyInfo rpcProxyInfo) {
        String clusterModel = properties.getClusterModel(rpcProxyInfo.getRealName());
        if (!StringUtils.hasText(clusterModel)) {
            throw new IllegalStateException(
                "starlight.client.config {clusterModel} is null, " + "please config it before run");
        }

        return clusterModel;
    }

    /**
     * Starlight Client bean name <1> when specify remoteUrl, clientBeanName will be "remoteUrl.StarlightClient</1> <2>
     * When not specify remoteUrl but specify name, clientBeanName will be "name.StarlightClient"</2> We create
     * StarlightClient bean at addressing level
     *
     * @param rpcProxyInfo
     * @return
     */
    private String clientBeanName(RpcProxyInfo rpcProxyInfo) {
        String remoteUrl = rpcProxyInfo.getRpcProxy().remoteUrl();
        if (!remoteUrl.isEmpty()) {
            if (remoteUrl.contains("://")) {
                remoteUrl = remoteUrl.split("://")[1];
            }
            return remoteUrl + CLIENT_BEAN_NAME_SUFFIX;
        }

        String name = rpcProxyInfo.getRealName();
        if (!name.isEmpty()) {
            return name + CLIENT_BEAN_NAME_SUFFIX;
        }

        throw new IllegalStateException(
            "Either name() or remoteUrl() must be provided in @" + RpcProxy.class.getSimpleName());
    }

    /**
     * Proxy bean name scopedTarget.starlightClientBeanName:{protocol}:interfaceName </2>
     *
     * @param rpcProxyInfo
     * @param proxyType
     * @return
     */
    private String proxyBeanName(RpcProxyInfo rpcProxyInfo, Class<?> proxyType) {
        StringBuilder buf = new StringBuilder(TARGET_NAME_PREFIX + clientBeanName(rpcProxyInfo) + ".");
        if (StringUtils.hasText(rpcProxyInfo.getRpcProxy().protocol())) {
            buf.append(rpcProxyInfo.getRpcProxy().protocol()).append(".");
        }
        buf.append(proxyType.getName());
        return buf.toString();
    }

    /**
     * Get Bean from spring ioc first Preventing repeated injection of beans
     *
     * @param clientBeanName
     * @return
     */
    private StarlightClient getClientFormIocCache(String clientBeanName) {
        StarlightClient starlightClient = null;
        try {
            starlightClient = beanFactory.getBean(clientBeanName, StarlightClient.class);
        } catch (BeansException e) {
            if (e instanceof NoSuchBeanDefinitionException) {
                LOGGER.info("Get bean {" + clientBeanName + "} failed, will create.");
            } else {
                throw e;
            }
        }
        return starlightClient;
    }

    /**
     * 去掉通过反射更改注解的属性值，改为额外的包装类 以解决jdk9模块化后java.base不暴露给unnamed module问题
     */
    private static class RpcProxyInfo {

        private RpcProxy rpcProxy;

        private String realName;

        public RpcProxyInfo(RpcProxy rpcProxy, String name) {
            this.rpcProxy = rpcProxy;
            this.realName = name;
        }

        public RpcProxy getRpcProxy() {
            return rpcProxy;
        }

        public void setRpcProxy(RpcProxy rpcProxy) {
            this.rpcProxy = rpcProxy;
        }

        public String getRealName() {
            return realName;
        }

        public void setRealName(String realName) {
            this.realName = realName;
        }
    }
}
