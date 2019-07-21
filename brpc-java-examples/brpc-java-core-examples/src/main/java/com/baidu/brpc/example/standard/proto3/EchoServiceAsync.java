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

package com.baidu.brpc.example.standard.proto3;

import java.util.concurrent.Future;
import com.baidu.brpc.client.RpcCallback;

/**
 * Created by baidu on 2017/9/23.
 */
public interface EchoServiceAsync extends EchoService {
    Future<Echo3.EchoResponse> echo(Echo3.EchoRequest request, RpcCallback<Echo3.EchoResponse> callback);
}
