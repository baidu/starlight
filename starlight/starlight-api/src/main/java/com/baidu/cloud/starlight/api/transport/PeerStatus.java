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
 
package com.baidu.cloud.starlight.api.transport;

import java.util.Objects;

/**
 * Created by liuruisen on 2020/11/27.
 */
public class PeerStatus {

    private Status status;

    /**
     * Record the status time, the value of time is System.currentTimeMillis()
     */
    private Long statusRecordTime;

    /**
     * Record the reason for changing to this status
     */
    private Object statusReason;

    public PeerStatus(Status status, Long statusRecordTime) {
        this.status = status;
        this.statusRecordTime = statusRecordTime;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Long getStatusRecordTime() {
        return statusRecordTime;
    }

    public void setStatusRecordTime(Long statusRecordTime) {
        this.statusRecordTime = statusRecordTime;
    }

    public Object getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(Object statusReason) {
        this.statusReason = statusReason;
    }

    /**
     * Status of the client or server ACTIVE <----> OUTLIER ACTIVE ----> SHUTTING DOWN ----> SHUTDOWN
     */
    public enum Status {
        /**
         * The peer is active
         */
        ACTIVE,

        /**
         * The peer is outlier
         */
        OUTLIER,

        /**
         * The peer is shutting down
         */
        SHUTTING_DOWN,

        /**
         * The peer is shutdown
         */
        SHUTDOWN
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PeerStatus{");
        sb.append("status=").append(status);
        sb.append(", statusRecordTime=").append(statusRecordTime);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PeerStatus that = (PeerStatus) o;
        return status == that.status && Objects.equals(statusRecordTime, that.statusRecordTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, statusRecordTime);
    }
}
