package com.baidu.brpc.spring.boot.autoconfigure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

@Getter
@Setter
@ConfigurationProperties(prefix = "brpc")
public class BrpcProperties implements EnvironmentAware {
    private BrpcConfig global;
    private Environment environment;

    public BrpcConfig getServiceConfig(Class<?> serviceInterface) {
        BrpcConfig brpcConfig = new BrpcConfig(global);
        StringBuilder sb = new StringBuilder(64);
        String prefix = sb.append("brpc.custom.")
                .append(serviceInterface.getName())
                .append(".")
                .toString();
        ReflectionUtils.doWithFields(RpcNamingConfig.class, new ReflectionUtils.FieldCallback() {
            @Override
            public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
                StringBuilder sb = new StringBuilder(128);
                String key = sb.append(prefix).append("naming.").append(field.getName()).toString();
                Object value = environment.getProperty(key, field.getType());
                if (value != null) {
                    try {
                        field.setAccessible(true);
                        field.set(brpcConfig.getNaming(), value);
                    } catch (Exception ex) {
                        throw new RuntimeException("set custom config failed", ex);
                    }
                }
            }
        });

        ReflectionUtils.doWithFields(RpcClientConfig.class, new ReflectionUtils.FieldCallback() {
            @Override
            public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
                StringBuilder sb = new StringBuilder(128);
                String key = sb.append(prefix).append("client.").append(field.getName()).toString();
                Object value = environment.getProperty(key, field.getType());
                if (value != null) {
                    try {
                        field.setAccessible(true);
                        field.set(brpcConfig.getClient(), value);
                    } catch (Exception ex) {
                        throw new RuntimeException("set custom config failed", ex);
                    }
                }
            }
        });

        ReflectionUtils.doWithFields(RpcServerConfig.class, new ReflectionUtils.FieldCallback() {
            @Override
            public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
                StringBuilder sb = new StringBuilder(128);
                String key = sb.append(prefix).append("server.").append(field.getName()).toString();
                Object value = environment.getProperty(key, field.getType());
                if (value != null) {
                    try {
                        field.setAccessible(true);
                        field.set(brpcConfig.getServer(), value);
                    } catch (Exception ex) {
                        throw new RuntimeException("set custom config failed", ex);
                    }
                }
            }
        });

        return brpcConfig;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
