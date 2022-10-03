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

/**
 * System Environment Utils Created by liuruisen on 2020/11/17.
 */
public class EnvUtils {

    public static int getCpuCores() {
        int cpuCores = Runtime.getRuntime().availableProcessors();
        if (isJarvisEnv() && StringUtils.isNumeric(System.getenv(Constants.EM_CPU_CORES))) {
            cpuCores = Integer.parseInt(System.getenv(Constants.EM_CPU_CORES)) / 10 * 4; // #单位约0.1核, 超发4
        }
        cpuCores = Math.max(8, cpuCores); // min core num = 8
        return cpuCores;
    }

    public static int getContainerThreadNum(int defaultValue) {
        if (isJarvisEnv() && hasThreadEnvVar()) {
            String threadNumber = System.getenv(Constants.EM_THREAD_NUMBER);
            return Integer.parseInt(threadNumber.trim()) * 10 / 6;
        } else {
            return defaultValue;
        }
    }

    public static boolean isJarvisEnv() {
        String emProductLine = System.getenv(Constants.EM_PRODUCT_LINE);
        String emPlatform = System.getenv(Constants.EM_PLATFORM);
        String emApp = System.getenv(Constants.EM_APP);

        return !StringUtils.isEmpty(emProductLine) && !StringUtils.isEmpty(emPlatform) && !StringUtils.isEmpty(emApp);
    }

    public static boolean hasThreadEnvVar() {
        String threadNum = System.getenv(Constants.EM_THREAD_NUMBER);
        return !StringUtils.isEmpty(threadNum);
    }

    public static boolean isJarvisOnline() {
        if (!isJarvisEnv()) {
            return false;
        }
        String emEnvType = System.getenv(Constants.EM_ENV_TYPE);
        return "ONLINE".equalsIgnoreCase(emEnvType);
    }

}
