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
 * Interface of Response Created by liuruisen on 2019/12/3.
 */
public abstract class Response extends AbstractMsgBase {

    public abstract int getStatus();

    public abstract void setStatus(int status);

    public abstract String getErrorMsg();

    public abstract void setErrorMsg(String errorMsg);

    public abstract Object getResult();

    public abstract void setResult(Object result);

    public Response(long id) {
        super(id);
    }

    public Response() {
        super();
    }

    /**
     * Save request information to obtain meta information, such as request kvAttachment
     * 
     * @return
     */
    public abstract Request getRequest();

    public abstract void setRequest(Request request);

    /**
     * When exception in response is not null, will throw this Exception
     * 
     * @return
     */
    public abstract Throwable getException();

    /**
     * Set exception, when exception instanceof RuntimeException will throw
     * 
     * @param exception
     */
    public abstract void setException(Throwable exception);
}
