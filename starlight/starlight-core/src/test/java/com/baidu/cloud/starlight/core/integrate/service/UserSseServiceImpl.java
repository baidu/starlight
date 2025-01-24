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

import com.baidu.cloud.starlight.api.rpc.sse.RpcSseEmitter;
import com.baidu.cloud.starlight.core.integrate.model.ExtInfo;
import com.baidu.cloud.starlight.core.integrate.model.User;
import com.baidu.cloud.starlight.core.rpc.sse.StarlightServerSseEmitter;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class UserSseServiceImpl implements UserSseService {
    @Override
    public RpcSseEmitter<User> getUserList() {
        RpcSseEmitter<User> sseEmitter = new StarlightServerSseEmitter<>(5 * 60 * 1000);

        try {
            sseEmitter.send(genUser(1L));
            sseEmitter.send(genUser(2L));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Thread thread = new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
                for (int i = 0; i < 3; i++) {
                    sseEmitter.send(genUser(i + 3L));
                }
                sseEmitter.complete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();
        return sseEmitter;
    }

    @Override
    public User addUser(User user) {
        return user;
    }

    User genUser(Long userId) {
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
}
