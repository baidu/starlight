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

import com.baidu.cloud.starlight.api.common.URI;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Stargate Request model. Stargate uses it for serialization as protocol data. Created by liuruisen on 2020/7/20.
 */
public class StargateRequest {

    private String id;

    private String methodName;

    private Object[] parameters;

    private Class<?>[] parameterTypes;

    private Map<String, Object> attachments;

    private URI uri;

    public StargateRequest(String id) {
        this.id = id;
    }

    public StargateRequest(String id, URI uri, String methodName, Class<?>[] parameterTypes, Object[] parameters) {
        this.uri = uri;
        this.methodName = methodName;
        this.id = id;
        this.parameterTypes = parameterTypes;
        this.parameters = parameters;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public void setSerial(String serial) {
        this.id = serial;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public String getMethodName() {
        return methodName;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public String getId() {
        return id;
    }

    public URI getUri() {
        return uri;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public String toString() {
        return "StargateRequest [methodName=" + methodName + ", parameterTypes=" + Arrays.toString(parameterTypes)
            + ", parameters=" + Arrays.toString(parameters);
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
