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

import com.baidu.cloud.starlight.api.model.Request;
import org.springframework.util.StringUtils;

/**
 * Created by liuruisen on 2021/11/8.
 */
public class RouteUtils {

    private static final String INSTANCE_SPLIT_1 = "\\.";

    private static final String INSTANCE_SPLIT_2 = "-";

    private static final String CONVERTED_INSTANCE_FORMAT = "%s-%s-%s";

    public static String convertedInstanceId(String instanceId) {
        if (StringUtils.isEmpty(instanceId)) {
            return instanceId;
        }
        String[] metaInfos = instanceId.split(INSTANCE_SPLIT_1);
        String[] platformInfos = metaInfos[1].split(INSTANCE_SPLIT_2);
        // instanceIdIndex-platform-logicIdc
        return String.format(CONVERTED_INSTANCE_FORMAT, metaInfos[0], platformInfos[1], platformInfos[4]);
    }


    public static String reqMsg(Request request) {
        return request.getId() + "#" + request.getServiceName() + "#" + request.getMethodName();
    }
}
