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
 
package com.baidu.cloud.starlight.api.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * 依据snowflake改造的算法，单节点使用，最大限度保持唯一
 */
public class IDGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(IDGenerator.class);

    /**
     * 起始的时间戳 2021-07-30 17:33:46
     */
    private static final long START_STMP = 1627637626000L;

    /**
     * 每一部分占用的位数 SEQUENCE_BIT: 序列号占用的位数 MACHINE_BIT: 机器标识占用的位数 DATACENTER_BIT: 机房占用的位数
     */
    private static final long SEQUENCE_BIT = 10;
    private static final long MACHINE_BIT = 16;
    private static final long DATACENTER_BIT = 2;

    /**
     * 每一部分的最大值
     */
    private static final long MAX_DATACENTER_NUM = -1L ^ (-1L << DATACENTER_BIT);
    private static final long MAX_MACHINE_NUM = -1L ^ (-1L << MACHINE_BIT);
    private static final long MAX_SEQUENCE = -1L ^ (-1L << SEQUENCE_BIT);

    /**
     * 每一部分向左的位移
     */
    private static final long MACHINE_LEFT = SEQUENCE_BIT;
    private static final long DATACENTER_LEFT = SEQUENCE_BIT + MACHINE_BIT;
    private static final long TIMESTMP_LEFT = DATACENTER_LEFT + DATACENTER_BIT;

    private long dataCenterId; // 数据中心
    private long machineId; // 机器标识
    private long sequence = 0L; // 序列号
    private long lastStamp = -1L; // 上一次时间戳

    private static IDGenerator idGenerator;

    public static IDGenerator getInstance() {
        if (idGenerator == null) {
            synchronized (IDGenerator.class) {
                if (idGenerator == null) {
                    Random rd = new Random();
                    long workerId = rd.nextInt(1 << 16);
                    long dataCenterId = rd.nextInt(3); // 机房随机数

                    // 获取当前ip,生成工作id
                    try {
                        String ip = NetUriUtils.getLocalHost();
                        if (ip != null) {
                            workerId = Long.parseLong(ip.replaceAll("\\.", ""));
                            workerId = workerId & 0XFFFF; // 取低16位
                        }
                    } catch (Throwable e) {
                        LOGGER.warn("Generate workerId from ip failed, caused by ", e);
                    }
                    idGenerator = new IDGenerator(dataCenterId, workerId);
                }
            }
        }
        return idGenerator;
    }

    private IDGenerator(long datacenterId, long machineId) {
        if (datacenterId > MAX_DATACENTER_NUM || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId can't be greater than MAX_DATACENTER_NUM or less than 0");
        }
        if (machineId > MAX_MACHINE_NUM || machineId < 0) {
            throw new IllegalArgumentException("machineId can't be greater than MAX_MACHINE_NUM or less than 0");
        }
        this.dataCenterId = datacenterId;
        this.machineId = machineId;
    }

    /**
     * 产生下一个ID
     *
     * @return
     */
    public synchronized long nextId() {
        long currStmp = getNewstamp();
        if (currStmp < lastStamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate id");
        }

        if (currStmp == lastStamp) {
            // 相同毫秒内，序列号自增
            sequence = (sequence + 1) & MAX_SEQUENCE;
            // 同一毫秒的序列数已经达到最大, 等待下一毫秒
            if (sequence == 0L) {
                currStmp = getNextMill();
            }
        } else {
            // 不同毫秒内，序列号置为0
            sequence = 0L;
        }

        lastStamp = currStmp;

        return (currStmp - START_STMP) << TIMESTMP_LEFT // 时间戳部分
            | dataCenterId << DATACENTER_LEFT // 机房随机数部分
            | machineId << MACHINE_LEFT // 机器IP标识部分
            | sequence; // 序列号部分
    }

    private long getNextMill() {
        long mill = getNewstamp();
        while (mill <= lastStamp) {
            mill = getNewstamp();
        }
        return mill;
    }

    private long getNewstamp() {
        return System.currentTimeMillis();
    }
}