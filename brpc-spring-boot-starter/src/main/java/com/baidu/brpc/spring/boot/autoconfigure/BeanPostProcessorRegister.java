package com.baidu.brpc.spring.boot.autoconfigure;

import com.baidu.brpc.spring.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * register {@link CommonAnnotationBeanPostProcessor} with ImportBeanDefinitionRegistrar,
 * instead of AutoConfigure class, so that CommonAnnotationBeanPostProcessor can be init before other beans.
 */
public class BeanPostProcessorRegister implements ImportBeanDefinitionRegistrar {
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry) {
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(CommonAnnotationBeanPostProcessor.class);
        beanDefinition.setSynthetic(true);
        MutablePropertyValues values = new MutablePropertyValues();
        values.addPropertyValue("callback", new SpringBootAnnotationResolver());
        beanDefinition.setPropertyValues(values);
        registry.registerBeanDefinition("commonAnnotationBeanPostProcessor", beanDefinition);
    }
}
