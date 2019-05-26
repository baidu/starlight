package com.baidu.brpc.spring.boot.autoconfigure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "brpc")
public class BrpcProperties {
    private BrpcConfig global;
    private Map<String, BrpcConfig> custom;
}
