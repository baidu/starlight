package com.baidu.brpc.spring.boot.autoconfigure;

import com.baidu.brpc.spring.annotation.RpcExporter;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
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

        for (Map.Entry<Class, String> entry : serviceExporterMap.entrySet()) {
            BeanDefinition bd = BeanDefinitionBuilder.rootBeanDefinition(ServiceExporterBean.class)
                    .addPropertyValue("serviceInterface", entry.getKey())
                    .addPropertyReference("serviceBean", entry.getValue())
                    .getBeanDefinition();
            String beanName = entry.getValue() + "ServiceExporterBean";
            registry.registerBeanDefinition(beanName, bd);
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
