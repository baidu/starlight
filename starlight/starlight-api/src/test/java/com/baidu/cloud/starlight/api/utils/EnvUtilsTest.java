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

import com.baidu.cloud.starlight.api.common.Constants;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static org.junit.Assert.*;

/**
 * Created by liuruisen on 2020/11/17.
 */
public class EnvUtilsTest {

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test
    public void getCpuCores() {

        assertEquals(Math.max(Runtime.getRuntime().availableProcessors(), 8), EnvUtils.getCpuCores());

        environmentVariables.set(Constants.EM_APP, "em-name");
        environmentVariables.set(Constants.EM_PLATFORM, "em-platform");
        environmentVariables.set(Constants.EM_PRODUCT_LINE, "em-product");

        environmentVariables.set(Constants.EM_CPU_CORES, "100");

        assertEquals(40, EnvUtils.getCpuCores());

        environmentVariables.set(Constants.EM_CPU_CORES, "10");
        assertEquals(8, EnvUtils.getCpuCores());
    }

    @Test
    public void isJarvisEnv() {
        assertFalse(EnvUtils.isJarvisEnv());

        environmentVariables.set(Constants.EM_APP, "em-name");
        environmentVariables.set(Constants.EM_PLATFORM, "em-platform");
        environmentVariables.set(Constants.EM_PRODUCT_LINE, "em-product");

        assertTrue(EnvUtils.isJarvisEnv());

    }
}