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
 
package com.baidu.cloud.starlight.springcloud.server.properties;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.rpc.config.TransportConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Global configuration for all RpcProxies Created by liuruisen on 2019-07-10.
 */
@ConfigurationProperties(prefix = StarlightServerProperties.PREFIX)
public class StarlightServerProperties {
    static final String PREFIX = "starlight.server";

    private String name;

    private Integer port;

    private String host;

    private boolean enable;

    private String protocols = Constants.SERVER_PROTOCOLS; // server support protocols

    private String compressType = "none";

    private String filters; // split by "," global level

    private Integer allIdleTimeout = Constants.ALL_IDLE_TIMEOUT_VALUE; // second

    private Integer ioThreadNum = Constants.DEFAULT_IO_THREADS_VALUE;

    private Integer acceptThreadNum = Constants.DEFAULT_ACCEPTOR_THREAD_VALUE;

    private Integer writeTimeoutMills = Constants.WRITE_TIMEOUT_VALUE;

    private GracefullyShutdown shutdown = new GracefullyShutdown();

    private Boolean connectKeepAliveEnable;

    private Integer registerDelay; // register after n s after SpringApplication started

    private Integer bizThreadNum; // env var > default 500

    private Integer ioRatio; // netty ioRatio, default 100

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProtocols() {
        return protocols;
    }

    public void setProtocols(String protocols) {
        this.protocols = protocols;
    }

    public String getCompressType() {
        return compressType;
    }

    public void setCompressType(String compressType) {
        this.compressType = compressType;
    }

    public String getFilters() {
        return filters;
    }

    public void setFilters(String filters) {
        this.filters = filters;
    }

    public Integer getAllIdleTimeout() {
        return allIdleTimeout;
    }

    public void setAllIdleTimeout(Integer allIdleTimeout) {
        this.allIdleTimeout = allIdleTimeout;
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

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getWriteTimeoutMills() {
        return writeTimeoutMills;
    }

    public void setWriteTimeoutMills(Integer writeTimeoutMills) {
        this.writeTimeoutMills = writeTimeoutMills;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public boolean getEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public GracefullyShutdown getShutdown() {
        return shutdown;
    }

    public void setShutdown(GracefullyShutdown shutdown) {
        this.shutdown = shutdown;
    }

    public Boolean getConnectKeepAliveEnable() {
        return connectKeepAliveEnable;
    }

    public void setConnectKeepAliveEnable(Boolean connectKeepAliveEnable) {
        this.connectKeepAliveEnable = connectKeepAliveEnable;
    }

    public Integer getRegisterDelay() {
        return registerDelay;
    }

    public void setRegisterDelay(Integer registerDelay) {
        this.registerDelay = registerDelay;
    }

    public Integer getBizThreadNum() {
        return bizThreadNum;
    }

    public void setBizThreadNum(Integer bizThreadNum) {
        this.bizThreadNum = bizThreadNum;
    }

    public Integer getIoRatio() {
        return ioRatio;
    }

    public void setIoRatio(Integer ioRatio) {
        this.ioRatio = ioRatio;
    }

    public TransportConfig transportConfig() {
        TransportConfig transportConfig = new TransportConfig();
        transportConfig.setAcceptThreadNum(this.acceptThreadNum);
        transportConfig.setIoThreadNum(this.ioThreadNum);
        transportConfig.setAllIdleTimeout(this.allIdleTimeout);
        transportConfig.setWriteTimeoutMills(this.writeTimeoutMills);
        transportConfig.setGracefullyShutdown(shutdown.getGracefully());
        transportConfig.setGracefullyQuietPeriod(shutdown.getQuietPeriod());
        transportConfig.setGracefullyTimeout(shutdown.getTimeout());
        transportConfig.setConnectKeepAliveEnable(this.connectKeepAliveEnable);
        transportConfig.setBizWorkThreadNum(this.bizThreadNum);
        transportConfig.setIoRatio(this.ioRatio);
        return transportConfig;
    }

    public class GracefullyShutdown {

        private boolean gracefully = true;

        private Integer quietPeriod = 2; // 2s

        private Integer timeout = 30; // 30s

        public boolean getGracefully() {
            return gracefully;
        }

        public void setGracefully(boolean gracefully) {
            this.gracefully = gracefully;
        }

        public Integer getQuietPeriod() {
            return quietPeriod;
        }

        public void setQuietPeriod(Integer quietPeriod) {
            this.quietPeriod = quietPeriod;
        }

        public Integer getTimeout() {
            return timeout;
        }

        public void setTimeout(Integer timeout) {
            this.timeout = timeout;
        }
    }
}
