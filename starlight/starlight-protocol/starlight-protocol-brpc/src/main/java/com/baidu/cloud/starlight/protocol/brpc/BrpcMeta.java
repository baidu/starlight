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

/**
 * Created by liuruisen on 2020/8/27.
 */
public class BrpcMeta {

    /**
     * message RpcMeta { optional RpcRequestMeta request = 1; optional RpcResponseMeta response = 2; optional Integer32
     * compress_type = 3; optional Integer64 correlation_id = 4; optional Integer32 attachment_size = 5; optional
     * ChunkInfo chunk_info = 6; optional bytes authentication_data = 7; optional StreamSettings stream_settings = 8; }
     */

    private BrpcRequestMeta request;

    private BrpcResponseMeta response;

    private Integer compressType;

    private Long correlationId;

    private Integer attachmentSize;

    private BrpcChunkInfo chunkInfo;

    private byte[] authenticationData;

    private BrpcStreamSettings streamSettings;

    public BrpcRequestMeta getRequest() {
        return request;
    }

    public void setRequest(BrpcRequestMeta request) {
        this.request = request;
    }

    public BrpcResponseMeta getResponse() {
        return response;
    }

    public void setResponse(BrpcResponseMeta response) {
        this.response = response;
    }

    public Integer getCompressType() {
        return compressType;
    }

    public void setCompressType(Integer compressType) {
        this.compressType = compressType;
    }

    public Long getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(Long correlationId) {
        this.correlationId = correlationId;
    }

    public Integer getAttachmentSize() {
        return attachmentSize;
    }

    public void setAttachmentSize(Integer attachmentSize) {
        this.attachmentSize = attachmentSize;
    }

    public BrpcChunkInfo getChunkInfo() {
        return chunkInfo;
    }

    public void setChunkInfo(BrpcChunkInfo chunkInfo) {
        this.chunkInfo = chunkInfo;
    }

    public byte[] getAuthenticationData() {
        return authenticationData;
    }

    public void setAuthenticationData(byte[] authenticationData) {
        this.authenticationData = authenticationData;
    }

    public BrpcStreamSettings getStreamSettings() {
        return streamSettings;
    }

    public void setStreamSettings(BrpcStreamSettings streamSettings) {
        this.streamSettings = streamSettings;
    }

    public static final class BrpcChunkInfo {

        /**
         * message ChunkInfo { required Integer64 stream_id = 1; required Integer64 chunk_id = 2; }
         */

        private Long streamId;

        private Long chunkId;

        public Long getStreamId() {
            return streamId;
        }

        public void setStreamId(Long streamId) {
            this.streamId = streamId;
        }

        public Long getChunkId() {
            return chunkId;
        }

        public void setChunkId(Long chunkId) {
            this.chunkId = chunkId;
        }
    }

}
