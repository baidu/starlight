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
 
package com.baidu.cloud.starlight.api.model;

/**
 * Created by liuruisen on 2020/2/6.
 */
public class RpcResponse extends Response {

    private int status;

    private String errorMsg;

    private Throwable exception;

    private Object result;

    private Request request;

    public RpcResponse(long id) {
        super(id);
    }

    public RpcResponse() {
        super();
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public String getErrorMsg() {
        return errorMsg;
    }

    @Override
    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    @Override
    public Object getResult() {
        return result;
    }

    @Override
    public void setResult(Object result) {
        this.result = result;
    }

    @Override
    public Request getRequest() {
        return request;
    }

    @Override
    public void setRequest(Request request) {
        this.request = request;
    }

    @Override
    public Throwable getException() {
        return this.exception;
    }

    @Override
    public void setException(Throwable exception) {
        this.exception = exception;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RpcResponse{");
        sb.append("status=").append(status);
        sb.append(", errorMsg='").append(errorMsg).append('\'');
        sb.append(", result=").append(result);
        sb.append(", request=").append(request);
        sb.append('}');
        return sb.toString();
    }
}
