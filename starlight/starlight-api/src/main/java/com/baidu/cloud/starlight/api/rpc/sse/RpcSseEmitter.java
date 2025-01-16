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
 
package com.baidu.cloud.starlight.api.rpc.sse;

import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;

import java.io.IOException;
import java.util.function.Consumer;

public interface RpcSseEmitter<T> {

    void init(RpcCallback rpcCallback);

    void send(T data) throws IOException;

    void send(SseEventBuilder<T> builder) throws IOException;

    void complete();

    void completeWithError(Throwable ex);

    void onTimeout(Runnable callback);

    void onError(Consumer<Throwable> callback);

    void onCompletion(Runnable callback);

    void onServerEvent(Consumer<ServerEvent> callback);

    void onData(Consumer<T> callback);

    void close();
}
