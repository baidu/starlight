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
 
package com.baidu.cloud.starlight.springcloud.client.properties;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by liuruisen on 2021/12/7.
 */
@ConfigurationProperties(prefix = StarlightRouteProperties.PREFIX)
public class StarlightRouteProperties {

    static final String PREFIX = "starlight.client.route";

    private final Map<String, String> labelSelectorCache = new ConcurrentHashMap<>();

    private Boolean enabled = true;

    private Boolean noInstanceFallBack = true; // 匹配到路由规则，未发现服务列表时，是否降级

    /**
     * label selector 静态配置，实现gravity label selector能力
     */
    private Selector labelSelector = new Selector();

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Selector getLabelSelector() {
        return labelSelector;
    }

    public void setLabelSelector(Selector labelSelector) {
        this.labelSelector = labelSelector;
    }

    public Boolean getNoInstanceFallBack() {
        return noInstanceFallBack;
    }

    public void setNoInstanceFallBack(Boolean noInstanceFallBack) {
        this.noInstanceFallBack = noInstanceFallBack;
    }

    public static class Selector {
        /**
         * Label selector configs for each service id.
         * <p>
         * For example, if you want to find only instances of service "hello_service" which match version 2.0.0 and
         * located at "nj02" region.
         * <p>
         * spring.cloud.gravity: discovery: provider-selector: hello_service: version=2.0.0 && region=nj02
         * <p>
         * or
         * <p>
         * starlight.client.route: selector: provider-selector: hello_service: version=2.0.0 && region=nj02
         * </p>
         * A label selector is an expression combined only by "&&" semantic separator. Each part of the expression is
         * composed by label + operator + value. Operators "=", "!=", are supported for single value expression, such as
         * `version=2.0.0`, `region != nj02`. And operators "in", "not in" are supported for set/collection value
         * expression, such as `region in (nj02, bj01)`.
         */
        private Map<String, String> providerSelector;

        /**
         * Global label selector applied for all service ids Selector keys in globalSelector has lower priority than
         * providerSelector
         */
        private String globalSelector;

        /**
         * providerSelector can be disable when enableProviderSelector is false
         */
        private Boolean enableProviderSelector;

        public Map<String, String> getProviderSelector() {
            return providerSelector;
        }

        public void setProviderSelector(Map<String, String> providerSelector) {
            this.providerSelector = providerSelector;
        }

        public String getGlobalSelector() {
            return globalSelector;
        }

        public void setGlobalSelector(String globalSelector) {
            this.globalSelector = globalSelector;
        }

        public Boolean getEnableProviderSelector() {
            return enableProviderSelector;
        }

        public void setEnableProviderSelector(boolean enableProviderSelector) {
            this.enableProviderSelector = enableProviderSelector;
        }
    }

    /**
     * Get label selector for service id
     *
     * @param serviceId service id
     * @return label selector (string), will never return null
     */
    public String getServiceLabelSelector(String serviceId) {
        String selector = labelSelectorCache.computeIfAbsent(serviceId, k -> {
            String labelSelector = "";
            if (this.labelSelector.getEnableProviderSelector() == null
                || !this.labelSelector.getEnableProviderSelector()) {
                labelSelector = this.labelSelector.globalSelector;
            } else {
                labelSelector = mergeSelector(this.labelSelector.globalSelector,
                    this.labelSelector.providerSelector.get(serviceId));
            }
            return labelSelector;
        });

        return selector;
    }

    public String mergeSelector(String globalSelector, String selector) {
        if (StringUtils.isBlank(selector)) {
            return globalSelector;
        }
        if (StringUtils.isBlank(globalSelector)) {
            return selector;
        }
        Set<String> uniqueKeys = new HashSet<>();
        return Stream.of(selector, globalSelector).map(this::splitSelectorsByKey).flatMap(Collection::stream).map(x -> {
            if (!uniqueKeys.contains(x[0])) {
                uniqueKeys.add(x[0]);
                return x[1];
            } else {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.joining(" && "));
    }

    private List<String[]> splitSelectorsByKey(String selector) {
        return Stream.of(selector.split("&&")).map(String::trim).filter(StringUtils::isNotBlank).map(x -> {
            for (String splitter : new String[] {" not in ", " in ", "!=", "="}) {
                int i = x.indexOf(splitter);
                if (i != -1) {
                    return new String[] {x.substring(0, i).trim(), x};
                }
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
