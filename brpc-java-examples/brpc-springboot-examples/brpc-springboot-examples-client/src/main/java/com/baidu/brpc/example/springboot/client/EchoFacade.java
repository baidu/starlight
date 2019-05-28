/*
 * Copyright (c) 2018 Baidu, Inc. All Rights Reserved.
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

package com.baidu.brpc.example.springboot.client;

import com.baidu.brpc.example.springboot.api.EchoRequest;
import com.baidu.brpc.example.springboot.api.EchoResponse;

import java.util.concurrent.Future;

public interface EchoFacade {
    EchoResponse echo(EchoRequest request);
    EchoResponse echo2(EchoRequest request);
    Future<EchoResponse> echo3(EchoRequest request);
}
