/*
 * Copyright (c) 2018 Baidu, Inc. All Rights Reserved.
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

package com.baidu.brpc.utils;

import com.baidu.brpc.protocol.BrpcMeta;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

@Slf4j
public class RpcMetaUtils {
    @Setter
    @Getter
    public static class RpcMetaInfo {
        private String serviceName;
        private String methodName;
    }

    public static RpcMetaInfo parseRpcMeta(Method targetMethod) {
        String serviceName;
        String methodName;
        BrpcMeta rpcMeta = targetMethod.getAnnotation(BrpcMeta.class);
        if (rpcMeta != null) {
            serviceName = rpcMeta.serviceName();
            methodName = rpcMeta.methodName();
        } else {
            serviceName = targetMethod.getDeclaringClass().getName();
            methodName = targetMethod.getName();
        }
        log.debug("serviceName={}, methodName={}", serviceName, methodName);
        RpcMetaInfo rpcMetaInfo = new RpcMetaInfo();
        rpcMetaInfo.setServiceName(serviceName);
        rpcMetaInfo.setMethodName(methodName);
        return rpcMetaInfo;
    }
}
