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

package com.baidu.brpc.protocol.nshead;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import struct.CString;
import struct.StructClass;
import struct.StructField;

/**
 * * ns产品线网络交互统一的包头，注释包含为(M)的为必须遵循的规范
 *
 * <pre>
 *     typedef struct _nshead_t
 *     {
 *         unsigned short id;              ///<id
 *         unsigned short version;         ///<版本号
 *         ///(M)由apache产生的logid，贯穿一次请求的所有网络交互
 *         unsigned int   log_id;
 *         ///(M)客户端标识，建议命名方式：产品名-模块名，比如"sp-ui", "mp3-as"
 *         char           provider[16];
 *         ///(M)特殊标识，标识一个包的起始
 *         unsigned int   magic_num;
 *         unsigned int   reserved;       ///<保留
 *         ///(M)head后请求数据的总长度
 *         unsigned int   body_len;
 *     } nshead_t
 * </pre>
 *
 * 2+2+4+16+4+4+4=36 byte in total.
 *
 */
@StructClass
public class NSHead {

    public static final int NSHEAD_LENGTH = 36;
    public static final int PROVIDER_LENGTH = 16;

    @StructField(order = 0)
    public short id = 0x00;

    @StructField(order = 1)
    public short version = 0x01;

    @StructField(order = 2)
    public int logId;

    // CHECKSTYLE:OFF
    @StructField(order = 3)
    public CString provider = new CString("", 16);

    @StructField(order = 4)
    public int magicNumber = 0xfb709394;

    @StructField(order = 5)
    public int reserved = 0;

    @StructField(order = 6)
    public int bodyLength = 0;
    // CHECKSTYLE:ON

    public NSHead(int logId, short id, short version, String provider, int bodyLength) {
        this.logId = logId;
        this.id = id;
        this.version = version;
        this.provider = new CString(provider, PROVIDER_LENGTH);
        this.bodyLength = bodyLength;
    }

    public NSHead(int logId, int bodyLength) {
        this.logId = logId;
        this.bodyLength = bodyLength;
    }

    public NSHead() {

    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(id).append(version).append(logId).append(magicNumber).append(reserved)
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
        if (!(obj instanceof NSHead)) {
            return false;
        }
        NSHead other = (NSHead) obj;
        return new EqualsBuilder().append(id, other.id).append(version, other.version).append(logId, other.logId)
                .append(reserved, other.reserved).append(bodyLength, other.bodyLength).isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("id", Integer.toHexString(id))
                .append("version", Integer.toHexString(version))
                .append("logId", Long.toHexString(logId) + "(" + logId + ")").append("provider", provider)
                .append("magicNumber", Integer.toHexString(magicNumber))
                .append("reserved", Integer.toHexString(reserved))
                .append("bodyLength", Integer.toHexString(bodyLength)).toString();
    }


}
