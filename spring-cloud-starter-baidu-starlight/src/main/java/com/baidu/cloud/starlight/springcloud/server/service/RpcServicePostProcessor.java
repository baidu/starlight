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

import com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants;
import com.baidu.cloud.starlight.springcloud.server.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.springframework.context.annotation.AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR;

/**
 * Used to register RpcServiceBean
 * Created by liuruisen on 2020/3/2.
 */
public class RpcServicePostProcessor implements BeanDefinitionRegistryPostProcessor,
        ApplicationContextAware, EnvironmentAware, ResourceLoaderAware, BeanClassLoaderAware, Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServicePostProcessor.class);

    private static final String RPC_SERVICE_BEAN_NAME_SUFFIX = "RpcService";

    private ApplicationContext applicationContext;

    private Environment environment;

    private ResourceLoader resourceLoader;

    private ClassLoader classLoader;

    private final Set<String> packagesToScan;

    public RpcServicePostProcessor(Set<String> packagesToScan) {
        this.packagesToScan = packagesToScan;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {

        Set<String> resolvedPackagesToScan = resolvePackagesToScan(packagesToScan);

        if (!resolvedPackagesToScan.isEmpty()) {
            registerRpcServiceBean(resolvedPackagesToScan, registry);
        } else {
            LOGGER.warn("Register RpcService Bean has been ignored: packagesToScan is empty!");
        }
    }

    private void registerRpcServiceBean(Set<String> packagesToScan, BeanDefinitionRegistry registry) {
        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry, false,
                environment, resourceLoader);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RpcService.class));
        String[] stringPackages = new String[packagesToScan.size()];
        stringPackages = packagesToScan.toArray(stringPackages);
        scanner.scan(stringPackages);

        Set<BeanDefinitionHolder> definitionHolders = findRpcServiceBeanHolder(packagesToScan, scanner, registry);

        for (BeanDefinitionHolder definitionHolder : definitionHolders) {
            BeanDefinition beanDefinition = definitionHolder.getBeanDefinition();
            Class<?> targetClass = ClassUtils.resolveClassName(beanDefinition.getBeanClassName(), classLoader);
            if (targetClass.getInterfaces().length == 0) {
                throw new IllegalArgumentException("@RpcService should be written on the implementation class, " +
                        "but is on {" + targetClass.getName() + "}");
            }
            Class<?> targetInterfaceClass = targetClass.getInterfaces()[0];
            RpcService rpcService = AnnotationUtils.findAnnotation(targetClass, RpcService.class);

            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(RpcServiceBean.class);
            builder.addPropertyReference("starlightServer", SpringCloudConstants.STARLIGHT_SERVER_NAME);
            builder.addPropertyValue("targetInterfaceClass", targetInterfaceClass);
            builder.addPropertyReference("target", definitionHolder.getBeanName());
            builder.addPropertyValue("annotation", rpcService);
            // builder.addPropertyValue("properties", applicationContext.getBean(StarlightServerProperties.class));

            BeanDefinition serviceBeanDefinition = builder.getBeanDefinition();
            String serviceBeanName = generateServiceBeanName(rpcService, targetInterfaceClass);
            registry.registerBeanDefinition(serviceBeanName, serviceBeanDefinition);
        }
    }

    private Set<BeanDefinitionHolder> findRpcServiceBeanHolder(Set<String> packagesToScan,
                                                               ClassPathBeanDefinitionScanner scanner,
                                                               BeanDefinitionRegistry registry) {
        BeanNameGenerator beanNameGenerator = resolveBeanNameGenerator(registry);
        Set<BeanDefinitionHolder> beanDefinitions = new LinkedHashSet<>();
        for (String basePackage : packagesToScan) {
            Set<BeanDefinition> candidates = scanner.findCandidateComponents(basePackage);
            for (BeanDefinition candidate : candidates) {
                String beanName = beanNameGenerator.generateBeanName(candidate, registry);
                BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(candidate, beanName);
                beanDefinitions.add(definitionHolder);
            }
        }
        return beanDefinitions;
    }


    /**
     * Get BeanNameGenerator
     *
     * @param registry
     * @return
     */
    private BeanNameGenerator resolveBeanNameGenerator(BeanDefinitionRegistry registry) {
        BeanNameGenerator beanNameGenerator = null;
        if (registry instanceof SingletonBeanRegistry) {
            SingletonBeanRegistry singletonBeanRegistry = SingletonBeanRegistry.class.cast(registry);
            beanNameGenerator =
                    (BeanNameGenerator) singletonBeanRegistry.getSingleton(CONFIGURATION_BEAN_NAME_GENERATOR);
        }

        if (beanNameGenerator == null) {
            LOGGER.info("BeanNameGenerator bean can't be found in BeanFactory with name ["
                    + CONFIGURATION_BEAN_NAME_GENERATOR + "]");
            LOGGER.info("BeanNameGenerator will be a instance of " +
                    AnnotationBeanNameGenerator.class.getName() +
                    " , it maybe a potential problem on bean name generation.");
            beanNameGenerator = new AnnotationBeanNameGenerator();
        }
        return beanNameGenerator;
    }

    /**
     * Generate ServiceBeanName
     *
     * @param rpcService
     * @param targetInterfaceClass
     * @return
     */
    private String generateServiceBeanName(RpcService rpcService, Class<?> targetInterfaceClass) {
        StringBuilder nameBuilder = new StringBuilder();
        nameBuilder.append(targetInterfaceClass.getName());
        nameBuilder.append(SpringCloudConstants.BEAN_NAME_SEPARATOR);
        nameBuilder.append(RPC_SERVICE_BEAN_NAME_SUFFIX);

        return nameBuilder.toString();
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // do nothing
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    private Set<String> resolvePackagesToScan(Set<String> packagesToScan) {
        Set<String> resolvedPackagesToScan = new LinkedHashSet<String>(packagesToScan.size());
        for (String packageToScan : packagesToScan) {
            if (StringUtils.hasText(packageToScan)) {
                String resolvedPackageToScan = environment.resolvePlaceholders(packageToScan.trim());
                resolvedPackagesToScan.add(resolvedPackageToScan);
            }
        }
        return resolvedPackagesToScan;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
