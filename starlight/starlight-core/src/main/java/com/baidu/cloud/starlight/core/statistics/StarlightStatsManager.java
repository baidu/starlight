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
 
package com.baidu.cloud.starlight.core.statistics;

import com.baidu.cloud.starlight.api.common.URI;
import com.baidu.cloud.starlight.api.utils.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by liuruisen on 2021/4/19.
 */
public class StarlightStatsManager {

    /**
     * key: ip:port
     *
     */
    private static final Map<String, StarlightStatistics> STARLIGHT_STATISTICS_MAP = new ConcurrentHashMap<>();

    /**
     * 参数为uri，考虑的是uri可以携带更多的配置信息
     * 
     * @param uri
     * @return
     */
    public static StarlightStatistics getOrCreateStats(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException("IP:PORT is empty when used it to create StarlightStats");
        }
        String clientKey = uri.getAddress(); // ip:port
        if (StringUtils.isEmpty(clientKey)) {
            throw new IllegalArgumentException("IP:PORT is empty when used it to create StarlightStats");
        }
        StarlightStatistics statistics = STARLIGHT_STATISTICS_MAP.get(clientKey);
        if (statistics == null) {
            synchronized (StarlightStatsManager.class) {
                statistics = STARLIGHT_STATISTICS_MAP.get(clientKey);
                if (statistics == null) {
                    statistics = new StarlightStatistics();
                    STARLIGHT_STATISTICS_MAP.put(clientKey, statistics);
                }
            }
        }

        return statistics;
    }

    public static StarlightStatistics getStats(URI uri) {
        String clientKey = uri.getAddress(); // ip:port
        return STARLIGHT_STATISTICS_MAP.get(clientKey);
    }

    public static void removeStats(URI uri) {
        String clientKey = uri.getAddress();
        STARLIGHT_STATISTICS_MAP.remove(clientKey);
    }

    public static StarlightStatistics getOrCreateStatsByHostPort(String hostPort) {
        if (StringUtils.isEmpty(hostPort)) {
            throw new IllegalArgumentException("IP:PORT is empty when used it to create StarlightStats");
        }
        StarlightStatistics statistics = STARLIGHT_STATISTICS_MAP.get(hostPort);
        if (statistics == null) {
            synchronized (StarlightStatsManager.class) {
                statistics = STARLIGHT_STATISTICS_MAP.get(hostPort);
                if (statistics == null) {
                    statistics = new StarlightStatistics();
                    STARLIGHT_STATISTICS_MAP.put(hostPort, statistics);
                }
            }
        }

        return statistics;
    }

    public static StarlightStatistics getStatsByHostPort(String hostPort) {
        return STARLIGHT_STATISTICS_MAP.get(hostPort);
    }

    public static void removeStatsByHostPort(String hostPort) {
        STARLIGHT_STATISTICS_MAP.remove(hostPort);
    }

}
