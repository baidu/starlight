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
import com.baidu.cloud.starlight.core.integrate.model.User;
import com.baidu.cloud.thirdparty.springframework.web.bind.annotation.GetMapping;
import com.baidu.cloud.thirdparty.springframework.web.bind.annotation.PostMapping;
import com.baidu.cloud.thirdparty.springframework.web.bind.annotation.RequestBody;

public interface UserSseService {

    @GetMapping("user/getUserList")
    RpcSseEmitter<User> getUserList();

    @PostMapping("user/addUser")
    User addUser(@RequestBody User user);

}
