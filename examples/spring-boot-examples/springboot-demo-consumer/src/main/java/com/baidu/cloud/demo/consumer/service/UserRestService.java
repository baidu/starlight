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
 
package com.baidu.cloud.demo.consumer.service;

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
 * Starlight Rest 风格支持
 * 
 * @Date 2022/9/28 21:16
 * @Created by liuruisen
 */
@RequestMapping("/user")
public interface UserRestService {

    @GetMapping
    User getUser(@RequestParam("userId") Long userId);

    @PutMapping
    User updateUser(@RequestBody User user);

    @DeleteMapping
    void deleteUser(@RequestParam("userId") Long userId);

    @PostMapping
    Long saveUser(@RequestBody User user);

    @GetMapping("/connect")
    void connect();

    @PostMapping("/multiparams")
    User multiParams(@RequestParam("userId") Long userId, @RequestBody User user);

    @GetMapping("/list")
    List<User> allUsers();

    @GetMapping("/name/{userId}")
    String userName(@PathVariable("userId") Long userId);

    @PostMapping("/map")
    Map<Long, User> userMap(@RequestParam("userId") Long userId, @RequestBody User user);

    @GetMapping("/exception")
    User userExp(Long userId);
}
