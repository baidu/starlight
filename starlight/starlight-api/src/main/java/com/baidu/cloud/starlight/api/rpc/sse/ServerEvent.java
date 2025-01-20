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

import com.baidu.cloud.thirdparty.apache.commons.lang3.StringUtils;

public class ServerEvent {

    public static final ServerEvent START_EVENT = new ServerEvent();

    public static final ServerEvent COMPLETE_EVENT = new ServerEvent();

    private String data;
    private String id;
    private String event;
    private long retry;
    private String comment;

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public long getRetry() {
        return retry;
    }

    public void setRetry(long retry) {
        this.retry = retry;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String toData() {
        StringBuffer sb = new StringBuffer();
        if (!StringUtils.isBlank(id)) {
            sb.append("id:").append(id).append('\n');
        }
        if (!StringUtils.isBlank(event)) {
            sb.append("event:").append(event).append('\n');
        }
        if (retry > 0) {
            sb.append("retry:").append(retry).append('\n');
        }
        if (!StringUtils.isBlank(comment)) {
            sb.append(":").append(comment).append('\n');
        }
        if (!StringUtils.isBlank(data)) {
            StringUtils.replace(data, "\n", "\ndata:");
            sb.append("data:").append(data).append("\n");
        }
        sb.append("\n");

        return sb.toString();
    }
}
