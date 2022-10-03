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
 
package com.baidu.cloud.demo.provider.service;

import com.baidu.cloud.demo.api.UserService;
import com.baidu.cloud.demo.api.model.Address;
import com.baidu.cloud.demo.api.model.ExtInfo;
import com.baidu.cloud.demo.api.model.Gender;
import com.baidu.cloud.demo.api.model.User;
import com.baidu.cloud.starlight.springcloud.server.annotation.RpcService;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@RpcService
public class UserServiceImpl implements UserService {

    @Override
    public User getUser(Long userId) {
        User user = new User();
        user.setUserId(userId);
        user.setUserName("User1");
        user.setBalance(1000.21d);
        user.setGender(Gender.MALE);
        List<String> tags = new LinkedList<>();
        tags.add("fgh");
        tags.add("123123");
        user.setTags(tags);
        List<ExtInfo> extInfos = new LinkedList<>();
        ExtInfo extInfo = new ExtInfo("hobby", "learn");
        extInfos.add(extInfo);
        user.setExtInfos(extInfos);
        user.setMap(Collections.singletonMap("key", new Address("Beijing")));
        return user;
    }

    @Override
    public User updateUser(User user) {
        return user;
    }

    @Override
    public void deleteUser(Long userId) {
        System.out.println("Delete user {" + userId + "}");
    }

    @Override
    public Long saveUser(User user) {
        return user.getUserId();
    }

    @Override
    public void connect() {
        System.out.println("Connect Success");
    }

    @Override
    public User multiParams(Long userId, User user) {
        if (user == null) {
            user = new User();
        }
        user.setUserId(userId == null ? 0L : userId);
        return user;
    }

    @Override
    public List<User> allUsers() {
        User user = new User();
        user.setUserId(1l);
        user.setUserName("u1");
        user.setBalance(1000.21d);
        List<String> tags = new LinkedList<>();
        tags.add("fgh");
        tags.add("123123");
        user.setTags(tags);
        List<ExtInfo> extInfos = new LinkedList<>();
        ExtInfo extInfo = new ExtInfo("hobby", "learn");
        extInfos.add(extInfo);
        user.setExtInfos(extInfos);
        user.setMap(Collections.singletonMap("key", new Address("heibei")));

        User user2 = new User();
        user2.setUserId(2l);
        user2.setUserName("u2");
        user2.setBalance(1000.21d);
        List<String> tags2 = new LinkedList<>();
        tags2.add("fgh");
        tags2.add("123123");
        user2.setTags(tags2);
        List<ExtInfo> extInfos2 = new LinkedList<>();
        ExtInfo extInfo2 = new ExtInfo("hobby", "learn");
        extInfos2.add(extInfo2);
        user2.setExtInfos(extInfos2);
        user2.setMap(Collections.singletonMap("Key1", new Address("Beijing")));

        List<User> users = new LinkedList<>();
        users.add(user);
        users.add(user2);
        return users;
    }

    @Override
    public String userName(Long userId) {
        return "User1";
    }

    @Override
    public Map<Long, User> userMap(Long userId, User user) {

        User user2 = new User();
        user2.setUserId(2L);
        user2.setUserName("u2");
        user2.setBalance(1000.21d);
        List<String> tags2 = new LinkedList<>();
        tags2.add("fgh");
        tags2.add("123123");
        user2.setTags(tags2);
        List<ExtInfo> extInfos2 = new LinkedList<>();
        ExtInfo extInfo2 = new ExtInfo("hobby", "learn");
        extInfos2.add(extInfo2);
        user2.setExtInfos(extInfos2);
        user2.setMap(Collections.singletonMap("Key1", new Address("Beijing")));

        return Collections.singletonMap(2L, user2);
    }

    @Override
    public User userExp(Long userId) {
        if (userId > 1000) {
            throw new StackOverflowError();
        }

        if (userId > 100) {
            throw new NullPointerException();
        }

        if (userId > 10) {
            throw new RuntimeException("Exception Occur");
        }

        return getUser(userId);

    }
}
