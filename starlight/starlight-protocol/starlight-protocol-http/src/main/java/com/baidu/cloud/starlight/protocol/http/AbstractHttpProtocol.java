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
 
package com.baidu.cloud.starlight.protocol.http;

import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.protocol.HeartbeatTrigger;
import com.baidu.cloud.starlight.api.protocol.Protocol;
import com.baidu.cloud.starlight.serialization.serializer.JsonSerializer;
import com.baidu.cloud.starlight.api.serialization.serializer.Serializer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * HttpProtocol Created by liuruisen on 2020/5/27.
 */
public abstract class AbstractHttpProtocol implements Protocol {

    private final static Serializer serializer = new JsonSerializer();

    public final static String X_STARLIGHT_ID = "x-starlight-id";

    public static final String CONTENT_TYPE_JSON = "application/json";

    public static final String ENCODING = "UTF-8";

    public static final Integer UNKNOW_STATUS = 99999;

    @Override
    public HeartbeatTrigger getHeartbeatTrigger() {
        return null;
    }

    @Override
    public Serializer getSerialize() {
        return serializer;
    }

    /**
     * If the request is servletRequest, such as # methodName(HttpServletRequest, HttpServletResponse) #
     * 
     * @param request
     * @return
     */
    public static boolean isServletRequest(Request request) {
        Class<?>[] paramTypes = request.getParamsTypes();
        if (paramTypes != null) {
            for (Class<?> paramClass : paramTypes) {
                if (HttpServletResponse.class.isAssignableFrom(paramClass)) {
                    return true;
                }

                if (HttpServletRequest.class.isAssignableFrom(paramClass)) {
                    return true;
                }
            }
        }
        return false;
    }
}
