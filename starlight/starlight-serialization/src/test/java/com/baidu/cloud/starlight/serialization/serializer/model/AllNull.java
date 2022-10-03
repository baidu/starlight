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

/**
 * Created by liuruisen on 2021/2/3.
 */
public class AllNull {

    List<Long> ids;

    Boolean isTrue;

    String strInfo;

    String strInfo2;

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }

    public Boolean getTrue() {
        return isTrue;
    }

    public void setTrue(Boolean aTrue) {
        isTrue = aTrue;
    }

    public String getStrInfo() {
        return strInfo;
    }

    public void setStrInfo(String strInfo) {
        this.strInfo = strInfo;
    }

    public String getStrInfo2() {
        return strInfo2;
    }

    public void setStrInfo2(String strInfo2) {
        this.strInfo2 = strInfo2;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AllNull{");
        sb.append("ids=").append(ids);
        sb.append(", isTrue=").append(isTrue);
        sb.append(", strInfo='").append(strInfo).append('\'');
        sb.append(", strInfo2='").append(strInfo2).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
