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

package com.baidu.brpc.naming.consul.model;

public class ConsulConstants {

    public static final int DEFAULT_CONSUL_INTERVAL = 30;

    public static final String CONSULINTERVAL = "consulInterval";

    public static final String LOOKUPINTERVAL = "lookupInterval";

    /**
     * service 最长存活周期（Time To Live），单位秒。 每个service会注册一个ttl类型的check，在最长TTL秒不发送心跳 就会将service变为不可用状态。
     */
    public static final int TTL                       = 30;
    /**
     * consul block 查询时 block的最长时间,单位，分钟
     */
    public static final int CONSUL_BLOCK_TIME_MINUTES = 10;

    /**
     * consul block 查询时 block的最长时间,单位，秒
     */
    public static final long CONSUL_BLOCK_TIME_SECONDS = CONSUL_BLOCK_TIME_MINUTES * 60;

    /**
     * 心跳周期，取ttl的2/3
     */
    public static final int HEARTBEAT_CIRCLE = 2000;

    /**
     * consul服务查询默认间隔时间。单位毫秒
     */
    public static final int DEFAULT_LOOKUP_INTERVAL = 20000;
}
