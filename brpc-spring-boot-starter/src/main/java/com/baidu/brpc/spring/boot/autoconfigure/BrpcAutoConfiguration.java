package com.baidu.brpc.spring.boot.autoconfigure;

import com.baidu.brpc.spring.boot.autoconfigure.config.BrpcProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@EnableConfigurationProperties(BrpcProperties.class)
@Configuration
@Import({BeanPostProcessorRegister.class, RpcExporterRegister.class})
public class BrpcAutoConfiguration {
}
