package com.baidu.brpc.example.springboot.server;

import com.baidu.brpc.interceptor.Interceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BrpcConfigs {
    @Bean
    public Interceptor customInterceptor() {
        return new CustomInterceptor();
    }

    @Bean
    public Interceptor customInterceptor2() {
        return new CustomInterceptor2();
    }
}
