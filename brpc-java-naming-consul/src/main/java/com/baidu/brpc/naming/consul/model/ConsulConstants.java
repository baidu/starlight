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
     * Time To Live, in seconds. Every service will register a ttl for health check.
     */
    public static final int TTL = 30;
    /**
     * consul block time for query params, measures in minutes.
     */
    public static final int CONSUL_BLOCK_TIME_MINUTES = 10;

    /**
     * consul block time for query params, measures in seconds.
     */
    public static final long CONSUL_BLOCK_TIME_SECONDS = CONSUL_BLOCK_TIME_MINUTES * 60;

    /**
     * heartbeat cycle time
     */
    public static final int HEARTBEAT_CIRCLE = 2000;

    /**
     * default check time for consul service
     */
    public static final int DEFAULT_LOOKUP_INTERVAL = 20000;
}
