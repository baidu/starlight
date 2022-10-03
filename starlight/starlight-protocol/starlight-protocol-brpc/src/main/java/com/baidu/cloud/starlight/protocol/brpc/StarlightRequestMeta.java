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
 
package com.baidu.cloud.starlight.protocol.brpc;

import java.util.Map;

/**
 * 适配Java生态，用于传输标准C版Brpc协议不支持，但Java生态里需要存在的meta Created by liuruisen on 2021/3/16.
 */
public class StarlightRequestMeta {

    /**
     * 标准Brpc协议仅支持传输 key type String, value type string的attach信息 为兼容从stargate迁移到brpc的已有<String, Object>场景，增加此字段扩展
     */
    private Map<String, Object> starlightExtFields;

    public Map<String, Object> getStarlightExtFields() {
        return starlightExtFields;
    }

    public void setStarlightExtFields(Map<String, Object> starlightExtFields) {
        this.starlightExtFields = starlightExtFields;
    }
}
