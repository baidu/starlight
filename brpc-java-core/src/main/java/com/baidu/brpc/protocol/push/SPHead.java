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

package com.baidu.brpc.protocol.push;

/**
 * * server push 网络交互统一的包头，注释包含为(M)的为必须遵循的规范
 */
public interface SPHead {

    /**
     * 请求返回结果
     */
    int TYPE_RESPONSE = 0;
    /**
     * 请求
     */
    int TYPE_REQUEST = 1;
    /**
     * server push的请求
     */
    int TYPE_SERVER_PUSH_REQUEST = 3;

    long getLogId();

    void setLogId(long logId);

    int getType();

    void setType(int type);

    @Override
    int hashCode();

    @Override
    boolean equals(Object obj);

}
