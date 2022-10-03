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

import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.statistics.Stats;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Starlight统计信息的汇总类 Client维度 Created by liuruisen on 2021/4/19.
 */
public class StarlightStatistics {

    private final Map<String, Stats> clientStatsMap;

    public StarlightStatistics() {
        this.clientStatsMap = new ConcurrentHashMap<>();
    }

    /**
     * Record statistics
     * 
     * @param request
     * @param response
     */
    public void record(Request request, Response response) {
        if (request == null || response == null) {
            return;
        }
        for (Map.Entry<String, Stats> clientStatsEntry : clientStatsMap.entrySet()) {
            if (clientStatsEntry.getValue() != null) {
                clientStatsEntry.getValue().record(request, response);
            }
        }
    }

    /**
     * Register stats
     * 
     * @param statsKey statsKey
     * @param stats
     */
    public void registerStats(String statsKey, Stats stats) {
        clientStatsMap.putIfAbsent(statsKey, stats);
    }

    /**
     * Discover stats by key
     * 
     * @param statsKey
     * @return
     */
    public Stats discoverStats(String statsKey) {
        return clientStatsMap.get(statsKey);
    }

    /**
     * Remove stats by key
     * 
     * @param statsKey
     * @return
     */
    public Stats removeStats(String statsKey) {
        return clientStatsMap.remove(statsKey);
    }
}
