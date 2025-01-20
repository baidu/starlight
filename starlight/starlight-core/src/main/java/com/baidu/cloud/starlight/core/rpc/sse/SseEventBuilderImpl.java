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

import com.baidu.cloud.starlight.api.rpc.sse.ServerEvent;
import com.baidu.cloud.starlight.api.rpc.sse.SseEventBuilder;
import com.baidu.cloud.starlight.api.serialization.serializer.Serializer;
import com.baidu.cloud.starlight.serialization.serializer.JsonSerializer;

import java.nio.charset.StandardCharsets;

public class SseEventBuilderImpl<T> implements SseEventBuilder<T> {

    private T data;
    private String id;
    private String event;
    private long retry;
    private String comment;

    private static final Serializer SERIALIZER = new JsonSerializer();

    /**
     * Add an SSE "id" line.
     */
    @Override
    public SseEventBuilder id(String id) {
        this.id = id;
        return this;
    }

    /**
     * Add an SSE "event" line.
     */
    @Override
    public SseEventBuilder event(String eventName) {
        this.event = eventName;
        return this;
    }

    /**
     * Add an SSE "retry" line.
     */
    @Override
    public SseEventBuilder retry(long retryMillis) {
        this.retry = retryMillis;
        return this;
    }

    /**
     * Add an SSE "comment" line.
     */
    @Override
    public SseEventBuilder comment(String comment) {
        this.comment = comment;
        return this;
    }

    /**
     * Add an SSE "data" line.
     */
    @Override
    public SseEventBuilder data(T data) {
        this.data = data;
        return this;
    }

    @Override
    public ServerEvent build() throws Exception {
        ServerEvent serverEvent = new ServerEvent();
        serverEvent.setId(id);
        serverEvent.setEvent(event);
        serverEvent.setRetry(retry);
        serverEvent.setComment(comment);

        if (data instanceof String) {
            serverEvent.setData((String) data);
        } else {
            // data 数据转换成 json string 进行传输
            byte[] serialize = SERIALIZER.serialize(data, data.getClass());
            String dataStr = new String(serialize, StandardCharsets.UTF_8);
            serverEvent.setData(dataStr);
        }
        return serverEvent;
    }
}
