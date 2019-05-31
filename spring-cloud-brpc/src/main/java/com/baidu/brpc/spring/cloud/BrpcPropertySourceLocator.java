package com.baidu.brpc.spring.cloud;

import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.HashMap;
import java.util.Map;

public class BrpcPropertySourceLocator implements PropertySourceLocator {

    @Override
    public PropertySource<?> locate(Environment environment) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("brpc.global.naming.namingServiceUrl", "springcloud://discovery");
        MapPropertySource propertySource = new MapPropertySource("brpc", properties);
        return propertySource;
    }
}
