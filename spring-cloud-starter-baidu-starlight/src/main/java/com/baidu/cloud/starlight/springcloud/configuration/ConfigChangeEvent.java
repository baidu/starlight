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
 
package com.baidu.cloud.starlight.springcloud.configuration;

/**
 * Created by liuruisen on 2021/9/6.
 */
public class ConfigChangeEvent {

    private Object configContent;

    private ChangeType changeType;

    public ConfigChangeEvent(Object configContent) {
        this.configContent = configContent;
    }

    public ConfigChangeEvent(Object configContent, ChangeType changeType) {
        this.configContent = configContent;
        this.changeType = changeType;
    }

    public Object getConfigContent() {
        return configContent;
    }

    public void setConfigContent(Object configContent) {
        this.configContent = configContent;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(ChangeType changeType) {
        this.changeType = changeType;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ConfigChangeEvent{");
        sb.append("configContent=").append(configContent);
        sb.append(", changeType=").append(changeType);
        sb.append('}');
        return sb.toString();
    }
}
