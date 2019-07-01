/*
 * Copyright (c) 2018 Baidu, Inc. All Rights Reserved.
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

package com.baidu.brpc.server;

import com.baidu.brpc.utils.BrpcConstants;

import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Created by wenweihu86 on 2017/4/24.
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
public class RpcServerOptions {
    // The keep alive
    private boolean keepAlive = true;
    private boolean tcpNoDelay = true;
    // so linger
    private int soLinger = 5;
    // backlog
    private int backlog = 1024;
    // receive buffer size
    private int receiveBufferSize = 1024 * 64;
    // send buffer size
    private int sendBufferSize = 1024 * 64;
    /**
     * an {@link IdleStateEvent} whose state is {@link IdleState#READER_IDLE}
     * will be triggered when no read was performed for the specified period of time.
     * Specify {@code 0} to disable.
     */
    private int readerIdleTime = 60;
    /**
     * an {@link IdleStateEvent} whose state is {@link IdleState#WRITER_IDLE}
     * will be triggered when no write was performed for the specified period of time.
     * Specify {@code 0} to disable.
     */
    private int writerIdleTime = 60;
    // keepAlive时间（second）
    private int keepAliveTime = 5;
    // acceptor threads, default use Netty default value
    private int acceptorThreadNum = 1;
    // io threads, default use Netty default value
    private int ioThreadNum = Runtime.getRuntime().availableProcessors();
    // real work threads
    private int workThreadNum = Runtime.getRuntime().availableProcessors();
    /**
     * io event type, netty or jdk
     */
    private int ioEventType = BrpcConstants.IO_EVENT_JDK;
    // The max size
    private int maxSize = Integer.MAX_VALUE;

    private int maxTryTimes = 1;

    public void setProtocolType(Integer protocolType) {
        this.protocolType = protocolType;
    }

    // server protocol type
    private Integer protocolType;
    private String encoding = "utf-8";
    // bns port name when deploys on Jarvis environment
    private String jarvisPortName;
    // naming service url
    private String namingServiceUrl = "";

    public RpcServerOptions(RpcServerOptions options) {
        this.copyFrom(options);
    }

    public void copyFrom(RpcServerOptions options) {
        this.acceptorThreadNum = options.acceptorThreadNum;
        this.backlog = options.backlog;
        this.encoding = options.encoding;
        this.ioThreadNum = options.ioThreadNum;
        this.jarvisPortName = options.jarvisPortName;
        this.keepAlive = options.keepAlive;
        this.keepAliveTime = options.keepAliveTime;
        this.maxSize = options.maxSize;
        this.namingServiceUrl = options.namingServiceUrl;
        this.protocolType = options.protocolType;
        this.readerIdleTime = options.readerIdleTime;
        this.receiveBufferSize = options.receiveBufferSize;
        this.sendBufferSize = options.sendBufferSize;
        this.soLinger = options.soLinger;
        this.tcpNoDelay = options.tcpNoDelay;
        this.workThreadNum = options.workThreadNum;
        this.writerIdleTime = options.writerIdleTime;
    }

}
