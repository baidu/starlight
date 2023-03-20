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

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Migrate from gravity label selector
 */
public class LabelParser {

    public LabelSelector parse(String label) {
        if (StringUtils.isEmpty(label)) {
            return null;
        }
        // 简单处理，&&分隔
        String[] labels = label.split("&&");

        List<LabelSelectorRequirement> lsr =
            Arrays.stream(labels).map(this::doParse).filter(Objects::nonNull).collect(Collectors.toList());

        LabelSelector ls = new LabelSelector();
        ls.setMatchExpressions(lsr);
        return ls;
    }

    private LabelSelectorRequirement doParse(String label) {

        label = label.trim();

        for (LabelOperator operator : LabelOperator.values()) {
            if (label.contains(operator.getOperator())) {
                String[] labelSplit = label.split(operator.getOperator(), 2);
                if (labelSplit.length != 2) {
                    return null;
                }
                String lhs = labelSplit[0].trim();
                String rhs = labelSplit[1].trim();
                // rhs去掉括号
                rhs = rhs.replaceAll("^\\(", "").replaceAll("\\)$", "");

                LabelSelectorRequirement ls = new LabelSelectorRequirement();
                ls.setKey(lhs);
                ls.setOperator(operator);
                ls.setValues(Arrays.stream(rhs.split(",")).map(String::trim).collect(Collectors.toList()));

                return ls;
            }
        }
        return null;
    }
}
