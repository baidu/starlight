package com.baidu.brpc.spring.cloud;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BrpcBootstrapConfiguration {

    @Bean
    public BrpcPropertySourceLocator brpcCustomPropertySourceLocator() {
        return new BrpcPropertySourceLocator();
    }
}
