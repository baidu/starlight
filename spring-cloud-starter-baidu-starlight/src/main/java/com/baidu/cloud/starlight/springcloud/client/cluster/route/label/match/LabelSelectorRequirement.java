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
 
package com.baidu.cloud.starlight.springcloud.client.cluster.route.label.match;

import com.baidu.cloud.thirdparty.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * Migrate from gravity label selector
 */
public class LabelSelectorRequirement {

    private String key;
    private LabelOperator operator;
    private List<String> values;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public LabelOperator getOperator() {
        return operator;
    }

    public void setOperator(LabelOperator operator) {
        this.operator = operator;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    public boolean labelValueMatch(String actual) {
        switch (operator) {
            case EQUAL:
                return !values.isEmpty() && StringUtils.isNotBlank(actual) && actual.equals(values.get(0));
            case NOT_EQUAL:
                return StringUtils.isBlank(actual) || !actual.equals(values.get(0));
            case IN:
                return StringUtils.isNotBlank(actual) && values.contains(actual);
            case NOT_IN:
                return StringUtils.isBlank(actual) || !values.contains(actual);
            default:
                return true;
        }
    }
}
