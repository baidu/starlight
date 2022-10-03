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
 
package com.baidu.cloud.starlight.core.rpc.generic;

import com.baidu.cloud.starlight.api.rpc.callback.Callback;
import com.baidu.cloud.starlight.api.exception.StarlightRpcException;

import java.util.concurrent.Future;

/**
 * Created by liuruisen on 2020/4/7.
 */
public interface AsyncGenericService extends GenericService {

    /**
     * Async rpc call: Future
     * 
     * @param method
     * @param args
     * @return
     * @throws StarlightRpcException
     */
    Future<Object> $invokeFuture(String method, Object[] args) throws StarlightRpcException;

    /**
     * Async rpc call: Callback
     * 
     * @param method
     * @param args method params exclude Callback
     * @param callback callback
     * @throws StarlightRpcException
     */
    void $invokeCallback(String method, Object[] args, Callback callback) throws StarlightRpcException;
}
