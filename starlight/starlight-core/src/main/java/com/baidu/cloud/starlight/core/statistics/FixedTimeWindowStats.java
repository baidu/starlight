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

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.statistics.CountStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Fixed time window statistics, expires will be cleared Created by liuruisen on 2021/4/18.
 */
public class FixedTimeWindowStats implements CountStats {

    private static final Logger LOGGER = LoggerFactory.getLogger(FixedTimeWindowStats.class);

    private static final int COUNT_INIT_VALUE = 0;

    private long timeWindowSize; // the size of time window: seconds

    private final AtomicReference<StatsPair> statsPair;

    public FixedTimeWindowStats(long timeWindowSize) {
        this.timeWindowSize = timeWindowSize;
        this.statsPair = new AtomicReference<>(new StatsPair());
    }

    @Override
    public Integer totalReqCount() {
        return recordAndGetReqCount(false, 0).getTotalReqCount();
    }

    @Override
    public Integer failReqCount() {
        return recordAndGetReqCount(false, 0).getFailReqCount();
    }

    @Override
    public Integer sucReqCount() {
        StatsPair pair = recordAndGetReqCount(false, 0);
        return pair.getTotalReqCount() - pair.getFailReqCount();
    }

    /**
     * Set window size, 可用于后序动态更改
     * 
     * @param timeWindowSize
     */
    public void setTimeWindowSize(long timeWindowSize) {
        this.timeWindowSize = timeWindowSize;
    }

    @Override
    public void recordReqCount(boolean success) {
        recordAndGetReqCount(success, 1);
    }

    @Override
    public void record(Request request, Response response) {
        boolean success = true;
        if (!Constants.SUCCESS_CODE.equals(response.getStatus())) {
            if (response.getStatus() > 1000 && response.getStatus() < 3000) { // StarlightRpcExp & TransportExp
                success = false;
            }
        }

        recordReqCount(success);
    }

    protected StatsPair recordAndGetReqCount(boolean success, int delta) {
        long startRecordTime = System.currentTimeMillis();

        StatsPair newPair = new StatsPair(); // FIXME 是否会带来GC次数增多
        StatsPair pair = null;
        do {
            pair = statsPair.get();

            long timeWinStartTime = pair.getTimeWinStartTime();
            newPair.setTimeWinStartTime(timeWinStartTime);
            long nextTimeWindStartTime = System.currentTimeMillis() - (timeWindowSize * 1000); // 预测的下一个时间窗口开始时间
            if (nextTimeWindStartTime > timeWinStartTime) { // 当前时间窗口已到期，开启下一个时间窗
                LOGGER.debug("The current window started at {} is expire , will creat new window", timeWinStartTime);
                newPair.setTimeWinStartTime(System.currentTimeMillis());
                newPair.setTotalReqCount(COUNT_INIT_VALUE + delta);
                newPair.setFailReqCount(COUNT_INIT_VALUE);
                if (!success) {
                    newPair.setFailReqCount(COUNT_INIT_VALUE + delta);
                }
            } else {
                newPair.setTotalReqCount(pair.getTotalReqCount() + delta);
                newPair.setFailReqCount(pair.getFailReqCount());
                if (!success) {
                    newPair.setFailReqCount(pair.getFailReqCount() + delta);
                }
            }
        } while (!statsPair.compareAndSet(pair, newPair)); // CAS thread-safe

        LOGGER.debug("Get or record stats cost {}", System.currentTimeMillis() - startRecordTime);
        return newPair;
    }

    /**
     * statistics
     */
    private static class StatsPair {
        private Integer totalReqCount; // TODO 后序接入Metrics框架可使用其定义的类

        private Integer failReqCount;

        private Long timeWinStartTime; // the timestamp of one fixed time window start time

        public StatsPair() {
            this.totalReqCount = 0;
            this.failReqCount = 0;
            this.timeWinStartTime = System.currentTimeMillis();
        }

        public StatsPair(Integer totalReqCount, Integer failReqCount, Long timestamp) {
            this.totalReqCount = totalReqCount;
            this.failReqCount = failReqCount;
            this.timeWinStartTime = timestamp;
        }

        public Integer getTotalReqCount() {
            return totalReqCount;
        }

        public void setTotalReqCount(Integer totalReqCount) {
            this.totalReqCount = totalReqCount;
        }

        public Integer getFailReqCount() {
            return failReqCount;
        }

        public void setFailReqCount(Integer failReqCount) {
            this.failReqCount = failReqCount;
        }

        public Long getTimeWinStartTime() {
            return timeWinStartTime;
        }

        public void setTimeWinStartTime(Long timeWinStartTime) {
            this.timeWinStartTime = timeWinStartTime;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            StatsPair statsPair = (StatsPair) o;
            return Objects.equals(totalReqCount, statsPair.totalReqCount)
                && Objects.equals(failReqCount, statsPair.failReqCount)
                && Objects.equals(timeWinStartTime, statsPair.timeWinStartTime);
        }

        @Override
        public int hashCode() {
            return Objects.hash(totalReqCount, failReqCount, timeWinStartTime);
        }
    }

}
