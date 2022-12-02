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
 
package com.baidu.cloud.starlight.springcloud.common;

import com.baidu.cloud.starlight.springcloud.common.RouteUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by liuruisen on 2021/11/8.
 */
public class XdsUtilsTest {

    @Test
    public void convertedInstanceId() {

        String instanceId = "1.test-test-test-test-yq.test.test";
        String expectId = "1-online-yq";

        assertEquals(expectId, RouteUtils.convertedInstanceId(instanceId));
    }
}