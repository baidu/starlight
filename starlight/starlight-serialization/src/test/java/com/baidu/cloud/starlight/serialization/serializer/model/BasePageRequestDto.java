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

import java.util.List;

public class BasePageRequestDto extends BaseDto {

    private int pageSize;
    private int pageNo;
    private List<OrderBy> orderBys;

    public BasePageRequestDto(Long appId, String appToken) {
        super(appId, appToken);
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getPageNo() {
        return pageNo;
    }

    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
    }

    public List<OrderBy> getOrderBys() {
        return orderBys;
    }

    public void setOrderBys(List<OrderBy> orderBys) {
        this.orderBys = orderBys;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BasePageRequestDto{");
        sb.append("pageSize=").append(pageSize);
        sb.append(", pageNo=").append(pageNo);
        sb.append(", orderBys=").append(orderBys);
        sb.append('}');
        return sb.toString();
    }
}