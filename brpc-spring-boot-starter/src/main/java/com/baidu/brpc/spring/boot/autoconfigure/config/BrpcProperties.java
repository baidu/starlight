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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BRPC Configuration.
 * <p>
 * Global defaults can be set under `brpc.global`.
 * Service-specific configs can be set under `brpc.custom.fully-qualified-service-name`.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "brpc")
public class BrpcProperties implements EnvironmentAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrpcProperties.class);
    private static final Pattern PATTERN_NOT_ALLOWED = Pattern.compile("[^a-zA-Z0-9\\-.]");
    @NestedConfigurationProperty
    private BrpcConfig global;
    @Setter
    private Environment environment;

    private static String camelToKebabCase(String str) {
        return str.replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase();
    }

    private static String normalizeName(String original) {
        String normalized = camelToKebabCase(original);
        if (PATTERN_NOT_ALLOWED.matcher(normalized).find()) {
            normalized = normalized.replaceAll("[^a-zA-Z0-9\\-.]", "");
            LOGGER.warn("Service name '{}' contains characters which are not allowed in "
                            + "Spring Boot 2.0 canonical properties, therefore a normalized form  "
                            + "will be used for config value binding. "
                            + "Please use '{}' as the config key for your service custom configuration.",
                    original, normalized);
        }
        return normalized;
    }

    private static void rewriteMap(Map<String, String> map) {
        if (map == null) {
            return;
        }
        Map<String, String> ret = new HashMap<>();
        for (int i = 0; i < map.size() / 2; i++) {
            String key = map.remove(i + ".key");
            String value = map.remove(i + ".value");
            if (StringUtils.isBlank(key) || StringUtils.isBlank(value)) {
                break;
            }
            ret.put(key, value);
        }
        map.clear();
        map.putAll(ret);
    }

    public BrpcConfig getServiceConfig(Class<?> serviceInterface) {
        BrpcConfig brpcConfig = new BrpcConfig(global);
        if (brpcConfig.getClient() == null) {
            brpcConfig.setClient(new RpcClientConfig());
        }
        if (brpcConfig.getServer() == null) {
            brpcConfig.setServer(new RpcServerConfig());
        }
        if (brpcConfig.getNaming() == null) {
            brpcConfig.setNaming(new RpcNamingConfig());
        }
        String prefix = "brpc.custom." + normalizeName(serviceInterface.getName()) + ".";
        Binder binder = Binder.get(environment);
        binder.bind(prefix + "client", Bindable.ofInstance(brpcConfig.getClient()));
        binder.bind(prefix + "server", Bindable.ofInstance(brpcConfig.getServer()));
        binder.bind(prefix + "naming", Bindable.ofInstance(brpcConfig.getNaming()));
        rewriteMap(brpcConfig.getNaming().getExtra());
        return brpcConfig;
    }
}
