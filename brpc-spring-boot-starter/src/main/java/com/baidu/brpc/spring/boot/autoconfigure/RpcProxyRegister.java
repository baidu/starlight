package com.baidu.brpc.spring.boot.autoconfigure;

import com.baidu.brpc.spring.annotation.RpcProxy;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AbstractTypeHierarchyTraversingFilter;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RpcProxyRegister extends AbstractRegister implements ImportBeanDefinitionRegistrar {
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        Collection<BeanDefinition> candidates = getCandidates(resourceLoader);

        Set<Class> references = candidates.stream()
                .flatMap(candidate -> {
                    Class<?> clazz = getClass(candidate.getBeanClassName());

                    return FieldUtils.getAllFieldsList(clazz)
                            .stream()
                            .filter(f -> f.getAnnotation(RpcProxy.class) != null)
                            .map(Field::getType);
                }).collect(Collectors.toSet());

        references.forEach(fieldType -> {
            BeanDefinition bd = BeanDefinitionBuilder.rootBeanDefinition(ProxyFactoryBean.class)
                    .addPropertyValue("serviceInterface", fieldType)
//                    .addPropertyReference("properties", "brpcProperties")
                    .getBeanDefinition();
            bd.setAttribute("factoryBeanObjectType", fieldType.getName());
            String name = fieldType.getSimpleName();
            registry.registerBeanDefinition(name, bd);
        });
    }

    private Collection<BeanDefinition> getCandidates(ResourceLoader resourceLoader) {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false, environment);
        scanner.addIncludeFilter(new AbstractTypeHierarchyTraversingFilter(true, false) {
            @Override
            protected boolean matchClassName(String className) {
                try {
                    Class<?> clazz = Class.forName(className);
                    List<Field> fields = FieldUtils.getAllFieldsList(clazz);
                    return fields
                            .stream()
                            .anyMatch( f -> f.getAnnotation(RpcProxy.class) != null);
                } catch (ClassNotFoundException e) {
                    throw new BeanInitializationException("class not found when match class name", e);
                }
            }
        });

        scanner.setResourceLoader(resourceLoader);
        return getBasePackages()
                .stream()
                .flatMap(basePackage -> scanner.findCandidateComponents(basePackage).stream())
                .collect(Collectors.toSet());
    }
}
