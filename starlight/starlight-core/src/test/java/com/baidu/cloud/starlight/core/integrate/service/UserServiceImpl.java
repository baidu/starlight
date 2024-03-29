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
 
package com.baidu.cloud.starlight.core.integrate.service;

import com.baidu.cloud.starlight.core.integrate.model.ExtInfo;
import com.baidu.cloud.starlight.core.integrate.model.User;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by liuruisen on 2020/3/6.
 */
public class UserServiceImpl implements UserService {

    @Override
    public User getUser(Long userId) {
        User user = new User();
        user.setUserId(userId);
        user.setUserName("User1");
        user.setBalance(1000.21d);
        List<String> tags = new LinkedList<>();
        tags.add("fgh");
        tags.add("123123");
        user.setTags(tags);
        List<ExtInfo> extInfos = new LinkedList<>();
        ExtInfo extInfo = new ExtInfo("hobby", "learn");
        extInfos.add(extInfo);
        user.setExtInfos(extInfos);
        return user;
    }

    @Override
    public User updateUser(User user) {
        return user;
    }

    @Override
    public void deleteUser(Long userId) {
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Delete user {" + userId + "}");
    }

    @Override
    public Long saveUser(User user) {
        return user.getUserId();
    }
}
