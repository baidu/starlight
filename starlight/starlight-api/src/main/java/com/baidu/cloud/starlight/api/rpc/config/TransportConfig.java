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
 
package com.baidu.cloud.starlight.api.rpc.config;

import java.util.Map;

/**
 * Rpc Core configuration Created by liuruisen on 2020/2/24.
 */
public class TransportConfig {

    private Integer connectTimeoutMills;

    private Integer writeTimeoutMills;

    private Integer requestTimeoutMills;

    private Integer readIdleTimeout; // second

    private Integer allIdleTimeout; // second

    private Integer maxHeartbeatTimes;

    private Integer ioThreadNum;

    private Integer acceptThreadNum;

    private String channelType;

    private Integer maxConnections;

    private Integer maxIdleConnections;

    private Integer minIdleConnections;

    private String bizThreadPoolName;

    private Integer bizWorkThreadNum;

    private Integer ioRatio;

    /********* gracefully shutdown config **********/
    private Boolean gracefullyShutdown;

    private Integer gracefullyQuietPeriod;

    private Integer gracefullyTimeout;

    private Boolean connectKeepAliveEnable;

    // Additional configuration: such as service governance configuration
    private Map<String, String> additional;

    public Integer getConnectTimeoutMills() {
        return connectTimeoutMills;
    }

    public void setConnectTimeoutMills(Integer connectTimeoutMills) {
        this.connectTimeoutMills = connectTimeoutMills;
    }

    public Integer getWriteTimeoutMills() {
        return writeTimeoutMills;
    }

    public void setWriteTimeoutMills(Integer writeTimeoutMills) {
        this.writeTimeoutMills = writeTimeoutMills;
    }

    public Integer getReadIdleTimeout() {
        return readIdleTimeout;
    }

    public void setReadIdleTimeout(Integer readIdleTimeout) {
        this.readIdleTimeout = readIdleTimeout;
    }

    public Integer getAllIdleTimeout() {
        return allIdleTimeout;
    }

    public void setAllIdleTimeout(Integer allIdleTimeout) {
        this.allIdleTimeout = allIdleTimeout;
    }

    public Integer getMaxHeartbeatTimes() {
        return maxHeartbeatTimes;
    }

    public void setMaxHeartbeatTimes(Integer maxHeartbeatTimes) {
        this.maxHeartbeatTimes = maxHeartbeatTimes;
    }

    public Integer getIoThreadNum() {
        return ioThreadNum;
    }

    public void setIoThreadNum(Integer ioThreadNum) {
        this.ioThreadNum = ioThreadNum;
    }

    public Integer getAcceptThreadNum() {
        return acceptThreadNum;
    }

    public void setAcceptThreadNum(Integer acceptThreadNum) {
        this.acceptThreadNum = acceptThreadNum;
    }

    public String getChannelType() {
        return channelType;
    }

    public void setChannelType(String channelType) {
        this.channelType = channelType;
    }

    public Integer getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(Integer maxConnections) {
        this.maxConnections = maxConnections;
    }

    public Integer getMaxIdleConnections() {
        return maxIdleConnections;
    }

    public void setMaxIdleConnections(Integer maxIdleConnections) {
        this.maxIdleConnections = maxIdleConnections;
    }

    public Integer getMinIdleConnections() {
        return minIdleConnections;
    }

    public void setMinIdleConnections(Integer minIdleConnections) {
        this.minIdleConnections = minIdleConnections;
    }

    public Integer getRequestTimeoutMills() {
        return requestTimeoutMills;
    }

    public void setRequestTimeoutMills(Integer requestTimeoutMills) {
        this.requestTimeoutMills = requestTimeoutMills;
    }

    public Boolean getGracefullyShutdown() {
        return gracefullyShutdown;
    }

    public void setGracefullyShutdown(Boolean gracefullyShutdown) {
        this.gracefullyShutdown = gracefullyShutdown;
    }

    public Integer getGracefullyQuietPeriod() {
        return gracefullyQuietPeriod;
    }

    public void setGracefullyQuietPeriod(Integer gracefullyQuietPeriod) {
        this.gracefullyQuietPeriod = gracefullyQuietPeriod;
    }

    public Integer getGracefullyTimeout() {
        return gracefullyTimeout;
    }

    public void setGracefullyTimeout(Integer gracefullyTimeout) {
        this.gracefullyTimeout = gracefullyTimeout;
    }

    public Boolean getConnectKeepAliveEnable() {
        return connectKeepAliveEnable;
    }

    public void setConnectKeepAliveEnable(Boolean connectKeepAliveEnable) {
        this.connectKeepAliveEnable = connectKeepAliveEnable;
    }

    public Map<String, String> getAdditional() {
        return additional;
    }

    public void setAdditional(Map<String, String> additional) {
        this.additional = additional;
    }

    public Integer getBizWorkThreadNum() {
        return bizWorkThreadNum;
    }

    public void setBizWorkThreadNum(Integer bizWorkThreadNum) {
        this.bizWorkThreadNum = bizWorkThreadNum;
    }

    public Integer getIoRatio() {
        return ioRatio;
    }

    public void setIoRatio(Integer ioRatio) {
        this.ioRatio = ioRatio;
    }

    public String getBizThreadPoolName() {
        return bizThreadPoolName;
    }

    public void setBizThreadPoolName(String bizThreadPoolName) {
        this.bizThreadPoolName = bizThreadPoolName;
    }
}
