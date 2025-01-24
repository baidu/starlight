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
 
package com.baidu.cloud.starlight.core.rpc.sse;

import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import com.baidu.cloud.starlight.api.rpc.sse.RpcSseEmitter;
import com.baidu.cloud.starlight.api.rpc.sse.ServerEvent;
import com.baidu.cloud.starlight.api.rpc.sse.SseEventBuilder;
import com.baidu.cloud.starlight.api.serialization.serializer.Serializer;
import com.baidu.cloud.starlight.core.rpc.callback.SseClientCallBack;
import com.baidu.cloud.starlight.serialization.serializer.JsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class StarlightClientSseEmitter<T> implements RpcSseEmitter<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StarlightClientSseEmitter.class);
    private Consumer<Throwable> onError;
    private Runnable onCompletion;
    private Consumer<ServerEvent> onServerEvent;
    private Consumer<T> onData;
    private static final Serializer SERIALIZER = new JsonSerializer();
    private Request request;
    private SseClientCallBack sseClientCallBack;

    private Type actualType = Object.class;

    private Throwable earlyError;

    private List<ServerEvent> earlyServerEvents = new ArrayList<>();

    public StarlightClientSseEmitter(Request request) {
        this.request = request;

        // 获取泛行实际类型
        Type genericReturnType = request.getGenericReturnType();
        if (genericReturnType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            if (actualTypeArguments.length == 1) {
                actualType = actualTypeArguments[0];
            }
        }
    }

    @Override
    public void init(RpcCallback rpcCallback) {
        throw new UnsupportedOperationException("client not support init()");
    }

    @Override
    public void send(T data) throws IOException {
        throw new UnsupportedOperationException("client not support send()");
    }

    @Override
    public void send(SseEventBuilder<T> builder) throws IOException {
        throw new UnsupportedOperationException("client not support send()");
    }

    @Override
    public void complete() {
        throw new UnsupportedOperationException("client not support complete()");
    }

    @Override
    public void completeWithError(Throwable ex) {
        throw new UnsupportedOperationException("client not support completeWithError()");
    }

    @Override
    public void onTimeout(Runnable callback) {
        throw new UnsupportedOperationException("client not support onTimeout()");
    }

    @Override
    public synchronized void onError(Consumer<Throwable> callback) {
        this.onError = callback;
        if (earlyError != null) {
            onError.accept(earlyError);
            earlyError = null;
        }
    }

    @Override
    public void onCompletion(Runnable callback) {
        this.onCompletion = callback;
    }

    @Override
    public synchronized void onServerEvent(Consumer<ServerEvent> callback) {
        this.onServerEvent = callback;

        earlyServerEvents.forEach(event -> pushServerEvent(event));
        earlyServerEvents.clear();
    }

    @Override
    public synchronized void onData(Consumer<T> callback) {
        this.onData = callback;

        earlyServerEvents.forEach(event -> pushServerEvent(event));
        earlyServerEvents.clear();
    }

    public synchronized void pushServerEvent(ServerEvent serverEvent) {
        try {
            if (onServerEvent != null) {
                onServerEvent.accept(serverEvent);
            } else if (onData != null) {
                String data = serverEvent.getData();

                T t = (T) SERIALIZER.deserialize(data.getBytes(StandardCharsets.UTF_8), actualType);
                onData.accept(t);
            } else {
                LOGGER.warn("no callback treat ServerEvent:{}", serverEvent);
                earlyServerEvents.add(serverEvent);
            }
        } catch (Throwable throwable) {
            LOGGER.warn("pushServerEvent fail.", throwable);
        }
    }

    public void triggerOnCompletion() {
        try {
            if (onCompletion != null) {
                onCompletion.run();
            }
        } catch (Throwable throwable) {
            LOGGER.warn("triggerOnCompletion fail.", throwable);
        }
    }

    public synchronized void triggerOnError(Throwable throwable) {
        try {
            if (onError != null) {
                onError.accept(throwable);
            } else {
                earlyError = throwable;
            }
        } catch (Throwable t) {
            LOGGER.warn("triggerOnError fail.", t);
        }
    }

    public void setSseClientCallBack(SseClientCallBack sseClientCallBack) {
        this.sseClientCallBack = sseClientCallBack;
    }

    /**
     * 给客户端一个强制断开连接的机会
     */
    @Override
    public void close() {
        sseClientCallBack.closeRpcChannel();
    }
}
