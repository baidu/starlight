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
 
package com.baidu.cloud.starlight.protocol.brpc;

import io.protostuff.Tag;

/**
 * Created by liuruisen on 2020/8/27.
 */
public class BrpcResponseMeta {

    /**
     * message RpcResponseMeta { optional Integer32 error_code = 1; optional string error_text = 2;
     *
     * optional StarlightResponseMeta starlight_response_meta = 1000; // starlight extension }
     */
    @Tag(1)
    private Integer errorCode;

    @Tag(2)
    private String errorText;

    @Tag(1000)
    private StarlightResponseMeta starlightResponseMeta;

    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorText() {
        return errorText;
    }

    public void setErrorText(String errorText) {
        this.errorText = errorText;
    }

    public StarlightResponseMeta getStarlightResponseMeta() {
        return starlightResponseMeta;
    }

    public void setStarlightResponseMeta(StarlightResponseMeta starlightResponseMeta) {
        this.starlightResponseMeta = starlightResponseMeta;
    }
}
