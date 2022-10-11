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
import com.baidu.cloud.thirdparty.springframework.web.bind.annotation.DeleteMapping;
import com.baidu.cloud.thirdparty.springframework.web.bind.annotation.GetMapping;
import com.baidu.cloud.thirdparty.springframework.web.bind.annotation.PathVariable;
import com.baidu.cloud.thirdparty.springframework.web.bind.annotation.PostMapping;
import com.baidu.cloud.thirdparty.springframework.web.bind.annotation.PutMapping;
import com.baidu.cloud.thirdparty.springframework.web.bind.annotation.RequestBody;
import com.baidu.cloud.thirdparty.springframework.web.bind.annotation.RequestMapping;
import com.baidu.cloud.thirdparty.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 默认情况无需增加SpringMVC注解，
 * 服务端对外提供Rest服务时才需要添加SpringMVC注解
 */
@RequestMapping("/user")
public interface UserService {

    /**
     * 单基本类型参数示例
     * 
     * @param userId
     * @return
     */
    @GetMapping
    User getUser(@RequestParam("userId") Long userId);

    @PutMapping
    User updateUser(@RequestBody User user);

    /**
     * 返回值void
     * 
     * @param userId
     */
    @DeleteMapping
    void deleteUser(@RequestParam("userId")Long userId);

    @PostMapping
    Long saveUser(@RequestBody User user);

    /**
     * 请求和响应参数均为空
     */
    @GetMapping("/connect")
    void connect();

    /**
     * 多参数 客户端需配置使用pb2-java序列化模式 - java api：ServiceConfig.setSerializeMode("pb2-java"); - spring boot 配置：
     * starlight.client.config.default.serializeMode=pb2-java
     * 
     * @param userId
     * @param user
     * @return
     */
    @PostMapping("/multiparams")
    User multiParams(@RequestParam("userId") Long userId, @RequestBody User user);

    /**
     * 返回List集合类 客户端需配置使用pb2-java序列化模式 - java api：ServiceConfig.setSerializeMode("pb2-java"); - spring boot 配置：
     * starlight.client.config.default.serializeMode=pb2-java
     * 
     * @return
     */
    @GetMapping("/list")
    List<User> allUsers();

    @GetMapping("/name/{userId}")
    String userName(@PathVariable("userId") Long userId);

    /**
     * 返回map集合类 客户端需配置使用pb2-java序列化模式 - java api：ServiceConfig.setSerializeMode("pb2-java"); - spring boot 配置：
     * starlight.client.config.default.serializeMode=pb2-java
     * 
     * @param userId
     * @param user
     * @return
     */
    @PostMapping("/map")
    Map<Long, User> userMap(@RequestParam("userId") Long userId, @RequestBody User user);

    /**
     * 方法抛出异常
     * 
     * @param userId
     * @return
     */
    @GetMapping("/exception")
    User userExp(Long userId);
}
