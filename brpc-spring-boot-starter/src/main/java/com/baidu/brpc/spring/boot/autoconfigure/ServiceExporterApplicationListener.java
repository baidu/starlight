package com.baidu.brpc.spring.boot.autoconfigure;

import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.naming.NamingServiceFactory;
import com.baidu.brpc.spring.RpcServiceExporter;
import com.baidu.brpc.spring.boot.autoconfigure.config.BrpcConfig;
import com.baidu.brpc.spring.boot.autoconfigure.config.BrpcProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Setter
@Getter
public class ServiceExporterApplicationListener implements
        ApplicationListener<ApplicationReadyEvent>, BeanFactoryAware, DisposableBean {
    private ListableBeanFactory beanFactory;
    private BrpcProperties properties;
    private Map<Integer, RpcServiceExporter> portServiceMap = new HashMap<>();

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Map<String, ServiceExporterBean> serviceBeanMap = beanFactory.getBeansOfType(ServiceExporterBean.class);
        for (Map.Entry<String, ServiceExporterBean> entry : serviceBeanMap.entrySet()) {
            BrpcConfig config = getServiceConfig(entry.getValue().getServiceInterface());
            Integer port = config.getServer().getPort();
            RpcServiceExporter exporter = portServiceMap.get(port);
            if (exporter == null) {
                exporter = new RpcServiceExporter();
                portServiceMap.put(port, exporter);
                exporter.setServicePort(port);
                exporter.copyFrom(config.getServer());
                exporter.setNamingServiceUrl(config.getNaming().getNamingServiceUrl());
                String namingServiceFactoryClassName = config.getNaming().getNamingServiceFactory();
                try {
                    NamingServiceFactory namingServiceFactory = (NamingServiceFactory)
                            Class.forName(namingServiceFactoryClassName).newInstance();
                    exporter.setNamingServiceFactory(namingServiceFactory);
                } catch (Exception ex) {
                    throw new RuntimeException("initialize naming factory failed", ex);
                }
                exporter.setGroup(config.getNaming().getGroup());
                exporter.setVersion(config.getNaming().getVersion());
                exporter.setIgnoreFailOfNamingService(config.getNaming().isIgnoreFailOfNamingService());
            }

            if (config.getServer() != null && config.getServer().isUseSharedThreadPool()) {
                exporter.getCustomOptionsServiceMap().put(config.getServer(), entry.getValue().getServiceBean());
            } else {
                exporter.getRegisterServices().add(entry.getValue().getServiceBean());
            }

            // interceptor
            if (config.getServer() != null
                    && StringUtils.isNoneBlank(config.getServer().getInterceptorBeanName())) {
                Interceptor interceptor = beanFactory.getBean(
                        config.getServer().getInterceptorBeanName(), Interceptor.class);
                if (exporter.getInterceptors() != null) {
                    exporter.getInterceptors().add(interceptor);
                } else {
                    exporter.setInterceptors(Arrays.asList(interceptor));
                }
            }
        }

        // start server
        for (RpcServiceExporter serviceExporter : portServiceMap.values()) {
            try {
                serviceExporter.afterPropertiesSet();
            } catch (Exception ex) {
                throw new RuntimeException("start brpc server failed");
            }
        }
    }

    public void destroy() {
        // stop server
        for (RpcServiceExporter serviceExporter : portServiceMap.values()) {
            try {
                serviceExporter.destroy();
            } catch (Exception ex) {
                throw new RuntimeException("stop brpc server failed");
            }
        }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (ListableBeanFactory) beanFactory;
    }

    private BrpcConfig getServiceConfig(Class<?> serviceInterface) {
        BrpcConfig brpcConfig;
        if (properties.getCustom() != null) {
            brpcConfig = properties.getCustom().get(serviceInterface.getName());
            if (brpcConfig != null) {
                return brpcConfig;
            }
        }
        brpcConfig = properties.getGlobal();
        return brpcConfig;
    }
}
