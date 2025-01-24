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
 
package com.baidu.cloud.starlight.protocol.http.springrest;

import com.baidu.cloud.thirdparty.springframework.web.bind.annotation.DeleteMapping;
import com.baidu.cloud.thirdparty.springframework.web.bind.annotation.GetMapping;
import com.baidu.cloud.thirdparty.springframework.web.bind.annotation.ModelAttribute;
import com.baidu.cloud.thirdparty.springframework.web.bind.annotation.PathVariable;
import com.baidu.cloud.thirdparty.springframework.web.bind.annotation.PostMapping;
import com.baidu.cloud.thirdparty.springframework.web.bind.annotation.PutMapping;
import com.baidu.cloud.thirdparty.springframework.web.bind.annotation.RequestBody;
import com.baidu.cloud.thirdparty.springframework.web.bind.annotation.RequestMapping;
import com.baidu.cloud.thirdparty.springframework.web.bind.annotation.RequestParam;
import com.baidu.cloud.starlight.protocol.http.User;

/**
 * Created by liuruisen on 2020/6/30.
 */
@RequestMapping("/spring-rest")
public interface SpringRestService {

    @GetMapping("/{id}")
    String get(@PathVariable("id") String id, @RequestParam("query") String query);

    @PutMapping
    String put(@RequestBody User user);

    @DeleteMapping("{id}")
    String delete(@PathVariable("id") String id);

    @PostMapping
    String post(@ModelAttribute User user);

    @GetMapping
    // not support querymap
    String getQueryMap(@RequestParam("query1") String query1, @RequestParam("query2") String query2);
}
