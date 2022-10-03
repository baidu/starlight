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

import com.baidu.cloud.thirdparty.protostuff.Tag;

import java.util.List;

/**
 * Created by liuruisen on 2020/8/27.
 */
public class BrpcRequestMeta {
    /**
     * message RpcRequestMeta { required string service_name = 1; required string method_name = 2; optional int64 log_id
     * = 3; optional int64 trace_id = 4; optional int64 span_id = 5; optional int64 parent_span_id = 6; repeated
     * RpcRequestMetaExtField ext_fields = 7;
     *
     * optional StarlightRequestMeta starlight_request_meta = 1000; // starlight extension }
     */
    @Tag(1)
    private String serviceName;

    @Tag(2)
    private String methodName;

    @Tag(3)
    private Long logId;

    @Tag(4)
    private Long traceId;

    @Tag(5)
    private Long spanId;

    @Tag(6)
    private Long parentSpanId;

    @Tag(7)
    private List<BrpcRequestMetaExt> extFields;

    @Tag(1000)
    private StarlightRequestMeta starlightRequestMeta; // 标准brpc的starlight扩展

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public Long getTraceId() {
        return traceId;
    }

    public void setTraceId(Long traceId) {
        this.traceId = traceId;
    }

    public Long getSpanId() {
        return spanId;
    }

    public void setSpanId(Long spanId) {
        this.spanId = spanId;
    }

    public Long getParentSpanId() {
        return parentSpanId;
    }

    public void setParentSpanId(Long parentSpanId) {
        this.parentSpanId = parentSpanId;
    }

    public List<BrpcRequestMetaExt> getExtFields() {
        return extFields;
    }

    public void setExtFields(List<BrpcRequestMetaExt> extFields) {
        this.extFields = extFields;
    }

    public StarlightRequestMeta getStarlightRequestMeta() {
        return starlightRequestMeta;
    }

    public void setStarlightRequestMeta(StarlightRequestMeta starlightRequestMeta) {
        this.starlightRequestMeta = starlightRequestMeta;
    }

    public static final class BrpcRequestMetaExt {

        /**
         * message RpcRequestMetaExtField { required string key = 1; required string value = 2; }
         */

        private String key;

        private String value;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
