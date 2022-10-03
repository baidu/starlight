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
 
package com.baidu.cloud.starlight.core.rpc.proxy;

import com.baidu.cloud.starlight.api.rpc.callback.Callback;

import java.util.concurrent.Future;

/**
 * Created by liuruisen on 2020/3/24.
 */
public interface TestService {

    String echo(String echo);

    String notCallback(String echo);

    void notFuture(String echo);

    void notCallback2(String echo, Callback callback);

    Future notFuture2(String echo);

}
