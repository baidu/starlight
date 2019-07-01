/*
 * Copyright (c) 2019 Baidu, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baidu.brpc.spring.boot.autoconfigure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "brpc")
public class BrpcProperties implements EnvironmentAware {
    private BrpcConfig global;
    private Environment environment;

    private static Map<String, String> extractMap(Environment env, String prefix) {
        Map<String, String> result = new HashMap<String, String>();
        int i = 0;
        while (true) {
            String key = env.getProperty(String.format("%s[%d].key", prefix, i));
            String value = env.getProperty(String.format("%s[%d].value", prefix, i));
            if (key == null || value == null) {
                break;
            }
            result.put(key, value);
            i++;
        }
        return result;
    }

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
                Object value;
                if (field.getName().equals("extra")) {
                    // extra field is a list of {key, value} objects
                    value = extractMap(environment, prefix + "naming.extra");
                } else {
                    StringBuilder sb = new StringBuilder(128);
                    String key = sb.append(prefix).append("naming.").append(field.getName()).toString();
                    value = environment.getProperty(key, field.getType());
                }
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
