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
 
package com.baidu.cloud.starlight.springcloud.client.outlier;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by liuruisen on 2021/9/7.
 */
public class OutlierDetectEventTest {

    @Test
    public void outlierReason() {
        OutlierDetectEvent event = new OutlierDetectEvent();
        System.out.println(event.outlierReason());
        event.setDetectInterval(3000);
        System.out.println(event.outlierReason());
        event.setReqCount(100);
        System.out.println(event.outlierReason());
        event.setSuccReqCount(88);
        System.out.println(event.outlierReason());
        event.setFailCount(12);
        System.out.println(event.outlierReason());
        event.setFailPercent(10);
        System.out.println(event.outlierReason());
        event.setDetectFailPercent(60);
        System.out.println(event.outlierReason());
        event.setDetectFailCount(66);
        System.out.println(event.outlierReason());
    }

}