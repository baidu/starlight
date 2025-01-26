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

/**
 * Support interface level config Created by liuruisen on 2020/12/10.
 */
public class InterfaceConfig {

    private Integer requestTimeoutMills;

    private Integer retryTimes; // interface level

    private String retryMethods; // interface level

    /**
     * The time unit of the time interval between two retries, which will be used to calculate the true delay time.
     */
    private Integer retryDelayTimeUnitMills; // default 100; interface level

    private String retryErrorCodes; // split by , default null

    public Integer getRequestTimeoutMills() {
        return requestTimeoutMills;
    }

    public void setRequestTimeoutMills(Integer requestTimeoutMills) {
        this.requestTimeoutMills = requestTimeoutMills;
    }

    public Integer getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(Integer retryTimes) {
        this.retryTimes = retryTimes;
    }

    public String getRetryMethods() {
        return retryMethods;
    }

    public void setRetryMethods(String retryMethods) {
        this.retryMethods = retryMethods;
    }

    public Integer getRetryDelayTimeUnitMills() {
        return retryDelayTimeUnitMills;
    }

    public void setRetryDelayTimeUnitMills(Integer retryDelayTimeUnitMills) {
        this.retryDelayTimeUnitMills = retryDelayTimeUnitMills;
    }

    public String getRetryErrorCodes() {
        return retryErrorCodes;
    }

    public void setRetryErrorCodes(String retryErrorCodes) {
        this.retryErrorCodes = retryErrorCodes;
    }
}
