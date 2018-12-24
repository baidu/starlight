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

import com.baidu.brpc.client.loadbalance.LoadBalanceType;
import com.baidu.brpc.protocol.Options;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by wenweihu86 on 2017/4/24.
 */
@Setter
@Getter
public class RpcClientOptions {

    private boolean isHttp = false;

    private int protocolType = Options.ProtocolType.PROTOCOL_BAIDU_STD_VALUE;

    private int connectTimeoutMillis = 1000;

    private int readTimeoutMillis = 1000;

    private int writeTimeoutMillis = 1000;

    private int maxTotalConnections = 8;

    private int minIdleConnections = 8;

    private int maxTryTimes = 3;

    private int loadBalanceType = LoadBalanceType.ROUND_ROBIN.getId();

    // for fair load balance strategy only
    private int latencyWindowSizeOfFairLoadBalance = 30;

    // for fair load balance strategy only
    // the ratio of activeInstancesNum/totalInstancesNum in brpc client, if this ratio not reached,
    // fair load balance will not start, just use random load balance strategy
    private float activeInstancesRatioOfFairLoadBalance = 0.5f;

    private int namingServiceUpdateIntervalMillis = 1000;

    // service group
    private String namingServiceGroup = "";

    // service version
    private String namingServiceVersion = "";

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
    private int keepAliveTime;

    // io threads, default use Netty default value
    private int ioThreadNum = Runtime.getRuntime().availableProcessors();

    // threads used for deserialize rpc response and execute the callback
    private int workThreadNum = Runtime.getRuntime().availableProcessors();

    // FastFutureStore's max size
    private int futureBufferSize = 1000000;

    private String encoding = "utf-8";

    private Options.CompressType compressType = Options.CompressType.COMPRESS_TYPE_NONE;
}
