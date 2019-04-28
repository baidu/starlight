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

import com.baidu.brpc.exceptions.BadSchemaException;
import io.netty.buffer.ByteBuf;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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
 * <p>
 * 2+2+4+16+4+4+4=36 byte in total.
 */
public class NSHead {

    public static final int NSHEAD_LENGTH = 36;
    public static final int NSHEAD_MAGIC_NUM = 0xfb709394;
    public static final int PROVIDER_LENGTH = 16;

    private static final byte[] ZEROS = new byte[PROVIDER_LENGTH];

    public short id = 0x00;

    public short version = 0x01;

    public int logId;

    public String provider = "";

    public int magicNumber = NSHEAD_MAGIC_NUM;

    public int reserved = 0;

    public int bodyLength = 0;

    public NSHead(int logId, short id, short version, String provider, int bodyLength) {
        this.logId = logId;
        this.id = id;
        this.version = version;
        if (provider != null) {
            this.provider = provider;
        }
        this.bodyLength = bodyLength;
    }

    public NSHead(int logId, int bodyLength) {
        this.logId = logId;
        this.bodyLength = bodyLength;
    }

    public NSHead() {
    }

    public static NSHead fromByteBuf(ByteBuf buf) throws BadSchemaException {
        NSHead head = new NSHead();
        if (buf.readableBytes() < NSHEAD_LENGTH) {
            throw new IllegalArgumentException("not enough bytes to read");
        }
        head.id = buf.readShortLE();
        head.version = buf.readShortLE();
        head.logId = buf.readIntLE();
        byte[] bytes = new byte[PROVIDER_LENGTH];
        buf.readBytes(bytes);
        int n = 0;
        while (n < bytes.length && bytes[n] != 0) {
            n++;
        }
        head.provider = new String(bytes, 0, n);
        head.magicNumber = buf.readIntLE();
        if (head.magicNumber != NSHEAD_MAGIC_NUM) {
            throw new BadSchemaException("nshead magic number does not match");
        }
        head.reserved = buf.readIntLE();
        head.bodyLength = buf.readIntLE();
        return head;
    }

    public byte[] toBytes() {
        ByteBuffer buf = ByteBuffer.allocate(NSHEAD_LENGTH);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putShort(id);
        buf.putShort(version);
        buf.putInt(logId);
        byte[] providerBytes = provider.getBytes();
        if (providerBytes.length >= PROVIDER_LENGTH) {
            buf.put(providerBytes, 0, PROVIDER_LENGTH);
        } else {
            buf.put(providerBytes, 0, providerBytes.length);
            buf.put(ZEROS, 0, PROVIDER_LENGTH - providerBytes.length);
        }
        buf.putInt(magicNumber);
        buf.putInt(reserved);
        buf.putInt(bodyLength);
        return buf.array();
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
