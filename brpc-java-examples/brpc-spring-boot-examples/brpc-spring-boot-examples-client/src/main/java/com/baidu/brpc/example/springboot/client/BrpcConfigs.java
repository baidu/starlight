package com.baidu.brpc.example.springboot.client;

import com.baidu.brpc.interceptor.Interceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BrpcConfigs {
    @Bean
    public Interceptor customInterceptor() {
        return new CustomInterceptor();
    }
}
