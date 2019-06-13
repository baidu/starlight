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

package com.baidu.brpc.protocol.push;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.baidu.brpc.exceptions.BadSchemaException;

import io.netty.buffer.ByteBuf;

/**
 * * server push 产品线网络交互统一的包头，注释包含为(M)的为必须遵循的规范
 *
 * <p>
 * 2+2+4+16+4+4+4=36 byte in total.
 */
public class SPHead {

    public static final int SPHEAD_LENGTH = 40;
    public static final int SPHEAD_MAGIC_NUM = 0xfb201906;
    public static final int PROVIDER_LENGTH = 16;

    public static final int TYPE_RESPONSE = 0;
    public static final int TYPE_REQUEST = 1;
    public static final int TYPE_CLIENT_REGISTER_REQUEST = 2;
    public static final int TYPE_SERVER_PUSH_REQUEST = 3;

    private static final byte[] ZEROS = new byte[PROVIDER_LENGTH];

    public short id = 0x00; // 2

    public short version = 0x01; // 2

    public long logId; // 8

    public String provider = ""; // 16

    public int magicNumber = SPHEAD_MAGIC_NUM; // 4

    public int type = 0; // 4

    public int bodyLength = 0; // 4

    public SPHead(int logId, short id, short version, String provider, int bodyLength) {
        this.logId = logId;
        this.id = id;
        this.version = version;
        if (provider != null) {
            this.provider = provider;
        }
        this.bodyLength = bodyLength;
    }

    public SPHead(int logId, int bodyLength) {
        this.logId = logId;
        this.bodyLength = bodyLength;
    }

    public SPHead() {
    }

    public static SPHead fromByteBuf(ByteBuf buf) throws BadSchemaException {
        SPHead head = new SPHead();
        if (buf.readableBytes() < SPHEAD_LENGTH) {
            throw new IllegalArgumentException("not enough bytes to read");
        }
        head.id = buf.readShortLE();
        head.version = buf.readShortLE();
        head.logId = buf.readLongLE();
        byte[] bytes = new byte[PROVIDER_LENGTH];
        buf.readBytes(bytes);
        int n = 0;
        while (n < bytes.length && bytes[n] != 0) {
            n++;
        }
        head.provider = new String(bytes, 0, n);
        head.magicNumber = buf.readIntLE();
        if (head.magicNumber != SPHEAD_MAGIC_NUM) {
            throw new BadSchemaException("nshead magic number does not match");
        }
        head.type = buf.readIntLE();
        head.bodyLength = buf.readIntLE();
        return head;
    }

    public byte[] toBytes() {
        ByteBuffer buf = ByteBuffer.allocate(SPHEAD_LENGTH);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putShort(id);
        buf.putShort(version);
        buf.putLong(logId);
        byte[] providerBytes = provider.getBytes();
        if (providerBytes.length >= PROVIDER_LENGTH) {
            buf.put(providerBytes, 0, PROVIDER_LENGTH);
        } else {
            buf.put(providerBytes, 0, providerBytes.length);
            buf.put(ZEROS, 0, PROVIDER_LENGTH - providerBytes.length);
        }
        buf.putInt(magicNumber);
        buf.putInt(type);
        buf.putInt(bodyLength);
        return buf.array();
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
        if (!(obj instanceof SPHead)) {
            return false;
        }
        SPHead other = (SPHead) obj;
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
