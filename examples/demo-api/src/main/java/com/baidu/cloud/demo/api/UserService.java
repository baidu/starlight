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
 
package com.baidu.cloud.demo.api;

import com.baidu.cloud.demo.api.model.User;

import java.util.List;
import java.util.Map;

public interface UserService {

    /**
     * 单基本类型参数示例
     * 
     * @param userId
     * @return
     */
    User getUser(Long userId);

    User updateUser(User user);

    /**
     * 返回值void
     * 
     * @param userId
     */
    void deleteUser(Long userId);

    Long saveUser(User user);

    /**
     * 请求和响应参数均为空
     */
    void connect();

    /**
     * 多参数 客户端需配置使用pb2-java序列化模式 - java api：ServiceConfig.setSerializeMode("pb2-java"); - spring boot 配置：
     * starlight.client.config.default.serializeMode=pb2-java
     * 
     * @param userId
     * @param user
     * @return
     */
    User multiParams(Long userId, User user);

    /**
     * 返回List集合类 客户端需配置使用pb2-java序列化模式 - java api：ServiceConfig.setSerializeMode("pb2-java"); - spring boot 配置：
     * starlight.client.config.default.serializeMode=pb2-java
     * 
     * @return
     */
    List<User> allUsers();

    String userName(Long userId);

    /**
     * 返回map集合类 客户端需配置使用pb2-java序列化模式 - java api：ServiceConfig.setSerializeMode("pb2-java"); - spring boot 配置：
     * starlight.client.config.default.serializeMode=pb2-java
     * 
     * @param userId
     * @param user
     * @return
     */
    Map<Long, User> userMap(Long userId, User user);

    /**
     * 方法抛出异常
     * 
     * @param userId
     * @return
     */
    User userExp(Long userId);
}
