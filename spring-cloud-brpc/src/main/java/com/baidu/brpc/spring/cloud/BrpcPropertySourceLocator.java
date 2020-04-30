package com.baidu.brpc.spring.cloud;

import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.HashMap;
import java.util.Map;

public class BrpcPropertySourceLocator implements PropertySourceLocator {
    public static final String ENV_NAMING_URL_KEY = "brpc.global.naming.namingServiceUrl";

    @Override
    public PropertySource<?> locate(Environment environment) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ENV_NAMING_URL_KEY,
                SpringCloudNamingFactory.NAMING_PREFIX + "://discovery");
        return new MapPropertySource("brpc", properties);
    }
}
