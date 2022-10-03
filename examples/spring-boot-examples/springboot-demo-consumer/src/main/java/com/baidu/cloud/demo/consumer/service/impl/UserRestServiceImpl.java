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
 
package com.baidu.cloud.demo.consumer.service.impl;

import com.baidu.cloud.demo.api.UserService;
import com.baidu.cloud.demo.api.model.User;
import com.baidu.cloud.demo.consumer.service.UserRestService;
import com.baidu.cloud.starlight.springcloud.client.annotation.RpcProxy;
import com.baidu.cloud.starlight.springcloud.server.annotation.RpcService;

import java.util.List;
import java.util.Map;

/**
 * @Date 2022/9/28 21:23
 * @Created by liuruisen
 */
@RpcService
public class UserRestServiceImpl implements UserRestService {

    /**
     * 不依赖注册发现，直连方式调用
     */
    @RpcProxy(remoteUrl = "brpc://localhost:8777")
    private UserService userService;

    @Override
    public User getUser(Long userId) {
        return userService.getUser(userId);
    }

    @Override
    public User updateUser(User user) {
        return userService.updateUser(user);
    }

    @Override
    public void deleteUser(Long userId) {
        userService.deleteUser(userId);
    }

    @Override
    public Long saveUser(User user) {
        return userService.saveUser(user);
    }

    @Override
    public void connect() {
        userService.connect();
    }

    @Override
    public User multiParams(Long userId, User user) {
        return userService.multiParams(userId, user);
    }

    @Override
    public List<User> allUsers() {
        return userService.allUsers();
    }

    @Override
    public String userName(Long userId) {
        return userService.userName(userId);
    }

    @Override
    public Map<Long, User> userMap(Long userId, User user) {
        return userService.userMap(userId, user);
    }

    @Override
    public User userExp(Long userId) {
        return userService.userExp(userId);
    }
}
