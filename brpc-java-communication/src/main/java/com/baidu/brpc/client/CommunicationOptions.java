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

package com.baidu.brpc.client;

import com.baidu.brpc.client.channel.ChannelType;
import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.protocol.Options;
import com.baidu.brpc.protocol.Protocol;
import com.baidu.brpc.protocol.ProtocolManager;
import com.baidu.brpc.utils.BrpcConstants;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wenweihu86 on 2017/4/24.
 */
@Getter
@Builder
@Slf4j
public class CommunicationOptions {
    private Protocol protocol;
    private List<Interceptor> interceptors = new ArrayList<Interceptor>();
    private int connectTimeoutMillis = 1000;
    private int readTimeoutMillis = 1000;
    private int writeTimeoutMillis = 1000;
    private int maxTotalConnections = 8;
    private int minIdleConnections = 8;
    private int maxTryTimes = 3;
    // Maximum time for connection idle, testWhileIdle needs to be true
    private long timeBetweenEvictionRunsMillis = 5 * 60 * 1000;
//    private int loadBalanceType = LoadBalanceStrategy.LOAD_BALANCE_FAIR;
    // for fair load balance strategy only
    private int latencyWindowSizeOfFairLoadBalance = 30;
    // for fair load balance strategy only
    // the ratio of activeInstancesNum/totalInstancesNum in brpc client, if this ratio not reached,
    // fair load balance will not start, just use random load balance strategy
//    private float activeInstancesRatioOfFairLoadBalance = 0.5f;
    private int healthyCheckIntervalMillis = 3000;
    // The keep alive
    private boolean keepAlive = true;
    private boolean reuseAddr = true;
    private boolean tcpNoDelay = true;
    // so linger
    private int soLinger = 5;
    // backlog
    private int backlog = 100;
    // receive buffer size
    private int receiveBufferSize = 1024 * 64;
    // send buffer size
    private int sendBufferSize = 1024 * 64;
    // keep alive time in seconds
    private int keepAliveTime = 60 * 5;
    // io threads, default use Netty default value
    private int ioThreadNum = Runtime.getRuntime().availableProcessors();
    // threads used for deserialize rpc response and execute the callback
    private int workThreadNum = Runtime.getRuntime().availableProcessors();
    /**
     * io event type, netty or jdk
     */
    private int ioEventType = BrpcConstants.IO_EVENT_JDK;
    // FastFutureStore's max size
    private int futureBufferSize = 1000000;
    private String encoding = "utf-8";
    private Options.CompressType compressType = Options.CompressType.COMPRESS_TYPE_NONE;
    private ChannelType channelType = ChannelType.POOLED_CONNECTION;
    private String clientName;

    // share worker thread poll and event thread pool between multi RpcClients
    private boolean globalThreadPoolSharing = false;

    public CommunicationOptions clone() {
        CommunicationOptions.CommunicationOptionsBuilder builder = CommunicationOptions.builder()
                .protocol(protocol)
                .interceptors(interceptors)
                .backlog(backlog)
                .channelType(channelType)
                .compressType(compressType)
                .connectTimeoutMillis(connectTimeoutMillis)
                .encoding(encoding)
                .healthyCheckIntervalMillis(healthyCheckIntervalMillis)
                .ioThreadNum(ioThreadNum)
                .keepAlive(keepAlive)
                .keepAliveTime(keepAliveTime)
                .maxTotalConnections(maxTotalConnections)
                .maxTryTimes(maxTryTimes)
                .minIdleConnections(minIdleConnections)
                .readTimeoutMillis(readTimeoutMillis)
                .receiveBufferSize(receiveBufferSize)
                .reuseAddr(reuseAddr)
                .sendBufferSize(sendBufferSize)
                .soLinger(soLinger)
                .tcpNoDelay(tcpNoDelay)
                .timeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis)
                .workThreadNum(workThreadNum)
                .writeTimeoutMillis(writeTimeoutMillis)
                .clientName(clientName)
                .globalThreadPoolSharing(globalThreadPoolSharing);
        return builder.build();
    }
}
