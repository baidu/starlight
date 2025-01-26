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
 
package com.baidu.cloud.starlight.springcloud.client.properties;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by liuruisen on 2021/9/13.
 */
public class OutlierConfigTest {

    @Test
    public void merge() {
        OutlierConfig outlierConfig = new OutlierConfig();
        outlierConfig.setEnabled(true);
        outlierConfig.setDetectInterval(60);
        outlierConfig.setBaseEjectTime(120);
        outlierConfig.setFailureCountThreshold(120);
        outlierConfig.setFailurePercentMinRequest(1);
        outlierConfig.setMaxEjectTime(600);
        assertEquals(true, outlierConfig.getEnabled());
        assertEquals(60, outlierConfig.getDetectInterval().intValue());

        OutlierConfig config = new OutlierConfig();
        config.setEnabled(false);
        config.setDetectInterval(120);
        config.setBaseEjectTime(300);
        config.setFailureCountThreshold(300);
        config.setFailurePercentMinRequest(3);

        outlierConfig.merge(config);
        assertEquals(false, outlierConfig.getEnabled());
        assertEquals(120, outlierConfig.getDetectInterval().intValue());

    }
}