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
 
package com.baidu.cloud.starlight.api.statistics;

/**
 * 请求Count统计信息 Created by liuruisen on 2021/9/18.
 */
public interface CountStats extends Stats {

    /**
     * Total request count
     * 
     * @return
     */
    Integer totalReqCount();

    /**
     * Failed request count
     * 
     * @return
     */
    Integer failReqCount();

    /**
     * Success request count
     * 
     * @return
     */
    Integer sucReqCount();

    /**
     * Record
     * 
     * @param success
     */
    void recordReqCount(boolean success);

}
