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

import java.lang.reflect.Field;

/**
 * Created by liuruisen on 2021/4/22.
 */
public class OutlierConfig {

    private Boolean enabled;

    private Integer detectInterval;

    private Integer failurePercentMinRequest;

    private Integer failurePercentThreshold;

    private Integer failureCountThreshold;

    private Integer baseEjectTime;

    private Integer maxEjectTime;

    private Integer maxEjectPercent;

    // 是否启用离线检测恢复，false将不进行心跳检测恢复
    private Boolean recoverByCheckEnabled;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getDetectInterval() {
        return detectInterval;
    }

    public void setDetectInterval(Integer detectInterval) {
        this.detectInterval = detectInterval;
    }

    public Integer getFailurePercentMinRequest() {
        return failurePercentMinRequest;
    }

    public void setFailurePercentMinRequest(Integer failurePercentMinRequest) {
        this.failurePercentMinRequest = failurePercentMinRequest;
    }

    public Integer getFailurePercentThreshold() {
        return failurePercentThreshold;
    }

    public void setFailurePercentThreshold(Integer failurePercentThreshold) {
        this.failurePercentThreshold = failurePercentThreshold;
    }

    public Integer getBaseEjectTime() {
        return baseEjectTime;
    }

    public void setBaseEjectTime(Integer baseEjectTime) {
        this.baseEjectTime = baseEjectTime;
    }

    public Integer getMaxEjectTime() {
        return maxEjectTime;
    }

    public void setMaxEjectTime(Integer maxEjectTime) {
        this.maxEjectTime = maxEjectTime;
    }

    public Integer getMaxEjectPercent() {
        return maxEjectPercent;
    }

    public void setMaxEjectPercent(Integer maxEjectPercent) {
        this.maxEjectPercent = maxEjectPercent;
    }

    public Integer getFailureCountThreshold() {
        return failureCountThreshold;
    }

    public void setFailureCountThreshold(Integer failureCountThreshold) {
        this.failureCountThreshold = failureCountThreshold;
    }

    public Boolean getRecoverByCheckEnabled() {
        return recoverByCheckEnabled;
    }

    public void setRecoverByCheckEnabled(Boolean recoverByCheckEnabled) {
        this.recoverByCheckEnabled = recoverByCheckEnabled;
    }

    /**
     * 合并，传入对象优先级高
     * 
     * @param outlierConfig
     */
    public void merge(OutlierConfig outlierConfig) {
        if (outlierConfig == null) {
            return;
        }
        try {
            for (Field field : this.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                if (field.get(outlierConfig) != null) {
                    field.set(this, field.get(outlierConfig));
                }
            }
        } catch (IllegalAccessException e) {
            // ignore
            throw new RuntimeException(e);
        }
    }

}
