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
 
package com.baidu.cloud.starlight.serialization.serializer.model;

import java.util.Objects;

public class OrderBy {
    public enum OrderType {
        ASC, DESC
    }

    private String property;

    private OrderType orderType;

    public OrderBy(final String property, final OrderType orderType) {

        this.property = property;
        this.orderType = orderType;
    }

    public String property() {
        return property;
    }

    public OrderType orderType() {
        return orderType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        OrderBy orderBy = (OrderBy) o;
        return Objects.equals(property, orderBy.property) && orderType == orderBy.orderType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(property, orderType);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("OrderBy{");
        sb.append("property='").append(property).append('\'');
        sb.append(", orderType=").append(orderType);
        sb.append('}');
        return sb.toString();
    }
}
