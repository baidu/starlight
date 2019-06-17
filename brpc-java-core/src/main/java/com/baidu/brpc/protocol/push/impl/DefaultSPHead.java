/*
 * Copyright (c) 2018 Baidu, Inc. All Rights Reserved.
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

package com.baidu.brpc.protocol.push.impl;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.baidu.brpc.protocol.push.SPHead;

/**
 * 2+2+8+16+4+4+4=36 byte in total.
 */
public class DefaultSPHead implements SPHead {

    public static final int SPHEAD_LENGTH = 40;
    public static final int SPHEAD_MAGIC_NUM = 0xfb201906;
    public static final int PROVIDER_LENGTH = 16;

    public static final byte[] ZEROS = new byte[PROVIDER_LENGTH];

    public short id = 0x00; // 2

    public short version = 0x01; // 2

    public long logId; // 8

    public String provider = ""; // 16

    public int magicNumber = SPHEAD_MAGIC_NUM; // 4

    public int type = 0; // 4

    public int bodyLength = 0; // 4

    public DefaultSPHead(int logId, short id, short version, String provider, int bodyLength) {
        this.logId = logId;
        this.id = id;
        this.version = version;
        if (provider != null) {
            this.provider = provider;
        }
        this.bodyLength = bodyLength;
    }

    public DefaultSPHead(int logId, int bodyLength) {
        this.logId = logId;
        this.bodyLength = bodyLength;
    }

    public DefaultSPHead() {
    }

    @Override
    public long getLogId() {
        return logId;
    }

    @Override
    public void setLogId(long logId) {
        this.logId = logId;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public void setType(int type) {
        this.type = type;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(id).append(version).append(logId).append(magicNumber).append(type)
                .append(bodyLength).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof DefaultSPHead)) {
            return false;
        }
        DefaultSPHead other = (DefaultSPHead) obj;
        return new EqualsBuilder().append(id, other.id).append(version, other.version).append(logId, other.logId)
                .append(type, other.type).append(bodyLength, other.bodyLength).isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("id", Integer.toHexString(id))
                .append("version", Integer.toHexString(version))
                .append("logId", Long.toHexString(logId) + "(" + logId + ")").append("provider", provider)
                .append("magicNumber", Integer.toHexString(magicNumber))
                .append("type", Integer.toHexString(type))
                .append("bodyLength", Integer.toHexString(bodyLength)).toString();
    }

}
