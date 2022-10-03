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
 
package com.baidu.cloud.starlight.protocol.stargate;

import java.util.HashMap;
import java.util.Map;

/**
 * Stargate Request model. Stargate uses it for serialization as protocol data. Created by liuruisen on 2020/7/20.
 */
public class StargateResponse {

    private String id;

    private Object exception;

    private Object result;

    private Map<String, Object> attachments;

    public StargateResponse(String id) {
        this.id = id;
    }

    public StargateResponse(String id, Object result, Throwable exception) {
        this.id = id;
        this.result = result;
        // if (error != null && error instanceof Throwable)
        // ((Throwable) error).initCause(null);
        this.exception = exception;
    }

    public Object getResult() {
        return result;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Throwable getException() {
        return (Throwable) exception;
    }

    public boolean hasException() {
        return exception != null;
    }

    @Override
    public String toString() {
        return "StargateResponse [result=" + result + ", exception=" + exception + "]";
    }

    public void setException(Throwable exp) {
        this.exception = exp;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Map<String, Object> getAttachments() {
        return attachments;
    }

    public Object getAttachment(String key) {
        if (attachments == null) {
            return null;
        }
        return attachments.get(key);
    }

    public void setAttachments(Map<String, Object> attachments) {
        this.attachments = attachments == null ? new HashMap<String, Object>() : attachments;
    }

    public void setAttachment(String key, Object value) {
        if (attachments == null) {
            attachments = new HashMap<String, Object>();
        }
        attachments.put(key, value);
    }
}
