package com.baidu.brpc.spring.boot.autoconfigure;

import com.baidu.brpc.spring.boot.autoconfigure.config.BrpcProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Slf4j
@EnableConfigurationProperties(value = {BrpcProperties.class})
@Import({RpcExporterRegister.class, RpcProxyRegister.class})
@Configuration
public class BrpcAutoConfiguration {
    @Autowired
    private BrpcProperties brpcProperties;

    @Bean
    public ServiceExporterApplicationListener serviceExporterApplicationListener() {
        ServiceExporterApplicationListener listener = new ServiceExporterApplicationListener();
        listener.setProperties(brpcProperties);
        return listener;
    }

}
