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
 
package com.baidu.cloud.starlight.springcloud.client.outlier;


/**
 * Record outlier reason
 * Created by liuruisen on 2021/9/6.
 */
public class OutlierDetectEvent {

    private Integer detectInterval;

    private Integer reqCount;

    private Integer succReqCount;

    private Integer failCount;

    private Integer detectFailCount;

    private Integer failPercent;

    private Integer detectFailPercent;

    public Integer getDetectInterval() {
        return detectInterval;
    }

    public void setDetectInterval(Integer detectInterval) {
        this.detectInterval = detectInterval;
    }

    public Integer getReqCount() {
        return reqCount;
    }

    public void setReqCount(Integer reqCount) {
        this.reqCount = reqCount;
    }

    public Integer getSuccReqCount() {
        return succReqCount;
    }

    public void setSuccReqCount(Integer succReqCount) {
        this.succReqCount = succReqCount;
    }

    public Integer getFailCount() {
        return failCount;
    }

    public void setFailCount(Integer failCount) {
        this.failCount = failCount;
    }

    public Integer getFailPercent() {
        return failPercent;
    }

    public void setFailPercent(Integer failPercent) {
        this.failPercent = failPercent;
    }

    public Integer getDetectFailCount() {
        return detectFailCount;
    }

    public void setDetectFailCount(Integer detectFailCount) {
        this.detectFailCount = detectFailCount;
    }

    public Integer getDetectFailPercent() {
        return detectFailPercent;
    }

    public void setDetectFailPercent(Integer detectFailPercent) {
        this.detectFailPercent = detectFailPercent;
    }

    public String outlierReason() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        builder.append("interval " + getDetectInterval() + "s, ");
        builder.append("reqcnt " + getReqCount() + ", ");
        builder.append("succcnt " + getSuccReqCount() + ", ");
        if (getDetectFailCount() != null) {
            builder.append("failcnt(trh" + getDetectFailCount() + ") " +getFailCount() + ", ");
        } else {
            builder.append("failcnt " + getFailCount() + ", ");
        }
        if (getDetectFailPercent() != null) {
            builder.append("failpct(trh" + getDetectFailPercent()+ "%) " + getFailPercent() + "%");
        }
        builder.append("]");

        return builder.toString();
    }
}
