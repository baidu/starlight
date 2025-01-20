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

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.model.RpcResponse;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import com.baidu.cloud.starlight.api.rpc.sse.RpcSseEmitter;
import com.baidu.cloud.starlight.api.rpc.sse.ServerEvent;
import com.baidu.cloud.starlight.api.rpc.sse.SseEventBuilder;
import com.baidu.cloud.starlight.protocol.http.springrest.sse.SpringRestSseProtocol;
import com.baidu.cloud.starlight.transport.utils.TimerHolder;
import com.baidu.cloud.thirdparty.netty.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Server端的SseEmitter
 *
 * @param <T>
 */
public class StarlightServerSseEmitter<T> implements RpcSseEmitter<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StarlightServerSseEmitter.class);
    private Timeout timeout;
    private long timeoutMills;

    private long lastSendTime = System.currentTimeMillis();

    private volatile boolean complete = false;

    private volatile Throwable failure;

    private RpcCallback rpcCallback;

    private volatile boolean initialized = false;

    /**
     * 如果emitter未初始化之前，发送的数据都会暂存这里
     */
    private final List<ServerEvent> earlySendAttempts = new ArrayList<>();

    /**
     * 如果emitter未初始化之前，complete标记会暂存这里
     */
    private volatile boolean earlyComplete = false;

    /**
     * 如果emitter未初始化之前，completeWithError的错误会暂存这里
     */
    private volatile Throwable earlyFailure;

    /**
     * 超时回调
     */
    private Runnable onTimeout;

    /**
     * 发送报错的时候回调
     */
    private Consumer<Throwable> onError;

    /**
     * emitter完成回调
     */
    private Runnable onCompletion;

    /**
     * 是否发送过response
     */
    private volatile boolean alreadySendResponse = false;

    public StarlightServerSseEmitter(long timeoutMills) {
        this.timeoutMills = timeoutMills;
        this.timeout = TimerHolder.getTimer().newTimeout(this::triggerTimeOut, timeoutMills, TimeUnit.MILLISECONDS);
    }

    private void triggerTimeOut(Timeout t) {
        if (complete) {
            return;
        }

        try {
            long period = System.currentTimeMillis() - lastSendTime;
            if (period >= timeoutMills) {
                // 超时了
                if (onTimeout != null) {
                    try {
                        onTimeout.run();
                    } catch (Exception e) {
                        LOGGER.warn("onTimeout fail.", e);
                    }
                }

                complete();
            } else {
                // 没有超时
                this.timeout = t.timer().newTimeout(this::triggerTimeOut, timeoutMills - period, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable throwable) {
            LOGGER.warn("triggerTimeOut fail.", throwable);
        }
    }

    public static <T> SseEventBuilder event() {
        return new SseEventBuilderImpl<T>();
    }

    @Override
    public synchronized void init(RpcCallback rpcCallback) {
        if (initialized) {
            throw new IllegalStateException("StarlightServerSseEmitter already initialized.");
        }
        initialized = true;
        this.rpcCallback = rpcCallback;

        try {
            earlySendAttempts.forEach(e -> sendInternal(e));
        } catch (Exception e) {
            completeWithError(e);
        } finally {
            earlySendAttempts.clear();
        }

        if (earlyComplete) {
            if (earlyFailure != null) {
                completeWithError(earlyFailure);
            } else {
                complete();
            }
        }

    }

    private void checkComplete() {
        if (initialized) {
            if (complete) {
                if (this.failure != null) {
                    throw new IllegalStateException("StarlightServerSseEmitter has already completedWithError",
                            failure);
                }
                throw new IllegalStateException("StarlightServerSseEmitter has already completed");
            }
        } else {
            if (earlyComplete) {
                if (this.earlyFailure != null) {
                    throw new IllegalStateException("StarlightServerSseEmitter has already completedWithError",
                            earlyFailure);
                }
                throw new IllegalStateException("StarlightServerSseEmitter has already completed");
            }
        }

    }

    private void sendInternal(ServerEvent serverEvent) {

        lastSendTime = System.currentTimeMillis();

        if (!initialized) {
            earlySendAttempts.add(serverEvent);
            return;
        }


        if (!alreadySendResponse) {
            // 如果还没发送过response，那么就发送开始事件
            alreadySendResponse = true;
            sendResponse(ServerEvent.START_EVENT);
        }

        sendResponse(serverEvent);
    }

    private void sendResponse(ServerEvent serverEvent){
        Request request = rpcCallback.getRequest();
        Response response = new RpcResponse(request.getId());
        response.setProtocolName(SpringRestSseProtocol.PROTOCOL_NAME);
        response.setStatus(Constants.SUCCESS_CODE);
        response.setResult(serverEvent);
        response.setReturnType(request.getReturnType());
        response.setGenericReturnType(request.getGenericReturnType());

        rpcCallback.onResponse(response);
    }

    @Override
    public synchronized void send(T data) throws IOException {
        checkComplete();

        try {
            ServerEvent event = event().data(data).build();
            sendInternal(event);
        } catch (Exception e) {
            completeWithError(e);
            throw new IOException("send event fail.", e);
        }
    }

    @Override
    public synchronized void send(SseEventBuilder<T> builder) throws IOException {
        checkComplete();

        try {
            sendInternal(builder.build());
        } catch (Exception e) {
            completeWithError(e);
            throw new IOException("send event fail.", e);
        }
    }

    @Override
    public synchronized void complete() {
        if (!initialized) {
            earlyComplete = true;
        }
        if (complete != true) {
            this.complete = true;
            if (onCompletion != null) {
                try {
                    onCompletion.run();
                } catch (Exception e) {
                    LOGGER.warn("onCompletion fail.", e);
                }
            }

            sendInternal(ServerEvent.COMPLETE_EVENT);

            if (timeout != null) {
                timeout.cancel();
            }
        }
    }

    @Override
    public synchronized void completeWithError(Throwable ex) {
        if (!initialized) {
            earlyComplete = true;
            earlyFailure = ex;
        }
        if (complete != true) {
            this.failure = ex;
            if (onError != null) {
                try {
                    onError.accept(ex);
                } catch (Exception e) {
                    LOGGER.warn("onError fail.", e);
                }
            }

            if (alreadySendResponse) {
                complete();
            } else {
                rpcCallback.onError(ex);
            }
        }
    }

    @Override
    public synchronized void onTimeout(Runnable callback) {
        this.onTimeout = callback;
    }

    @Override
    public synchronized void onError(Consumer<Throwable> callback) {
        this.onError = callback;
    }

    @Override
    public synchronized void onCompletion(Runnable callback) {
        this.onCompletion = callback;
    }

    @Override
    public void onServerEvent(Consumer<ServerEvent> callback) {
        throw new UnsupportedOperationException("server not support onServerEvent() callback");
    }

    @Override
    public void onData(Consumer<T> callback) {
        throw new UnsupportedOperationException("server not support onData() callback");
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException(
                "server not support close() method, please use complete()/completeWithError() method");
    }

}
