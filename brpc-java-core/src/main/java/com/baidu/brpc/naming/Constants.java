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
package com.baidu.brpc.naming;

public class Constants {
    public static final String GROUP = "group";
    public static final String VERSION = "version";

    // update timer interval for pull mode
    public static final String INTERVAL = "interval";
    public static final int DEFAULT_INTERVAL = 5000;

    public static final String SLEEP_TIME_MS = "sleepTimeMs";
    public static final int DEFAULT_SLEEP_TIME_MS = 1000;

    public static final String MAX_TRY_TIMES = "maxTryTimes";
    public static final int DEFAULT_MAX_TRY_TIMES = 3;

    public static final String CONNECT_TIMEOUT_MS = "connectTimeoutMs";
    public static final int DEFAULT_CONNECT_TIMEOUT_MS = 1000;

    public static final String SESSION_TIMEOUT_MS = "sessionTimeoutMs";
    public static final int DEFAULT_SESSION_TIMEOUT_MS = 60000;

    public static final String DEFAULT_PATH = "";
}
