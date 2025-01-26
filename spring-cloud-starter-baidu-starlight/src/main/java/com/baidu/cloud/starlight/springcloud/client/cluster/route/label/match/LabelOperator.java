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

/**
 * Migrate from gravity labelselector
 */
public enum LabelOperator {

    NOT_EQUAL("!="),
    EQUAL("="),
    NOT_IN(" not in"),
    IN(" in");

    private String operator;

    LabelOperator(String operator) {
        this.operator = operator;
    }

    public String getOperator() {
        return operator;
    }
}
