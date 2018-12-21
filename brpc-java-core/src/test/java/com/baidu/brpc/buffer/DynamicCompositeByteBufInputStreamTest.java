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

package com.baidu.brpc.buffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class DynamicCompositeByteBufInputStreamTest {
    @Test
    public void testRead() throws IOException {
        ByteBuf buf = Unpooled.buffer(12);
        buf.writeByte(1);
        buf.writeByte(2);
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf(buf);
        DynamicCompositeByteBufInputStream inputStream = new DynamicCompositeByteBufInputStream(compositeByteBuf);
        Assert.assertTrue(inputStream != null);
        int b = inputStream.read();
        Assert.assertTrue(b == 1);

        Assert.assertTrue(buf.refCnt() == 1);
        inputStream.close();
        Assert.assertTrue(buf.refCnt() == 1);
        compositeByteBuf.release();
        Assert.assertTrue(buf.refCnt() == 0);
        inputStream.close();
    }

    @Test
    public void testReadBytes() throws IOException {
        byte[] bytes = "hello".getBytes();
        ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf(buf);
        DynamicCompositeByteBufInputStream inputStream = new DynamicCompositeByteBufInputStream(compositeByteBuf);

        byte[] dstBytes = new byte[bytes.length];
        inputStream.read(dstBytes, 0, bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            Assert.assertTrue(dstBytes[i] == bytes[i]);
        }
        compositeByteBuf.release();
        inputStream.close();
    }

    @Test
    public void testSkip() throws IOException {
        ByteBuf buf = Unpooled.buffer(12);
        buf.writeInt(12);
        buf.writeInt(23);
        buf.writeInt(34);
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf(buf);
        DynamicCompositeByteBufInputStream inputStream = new DynamicCompositeByteBufInputStream(compositeByteBuf);
        inputStream.skip(4);
        int i = inputStream.readInt();
        Assert.assertTrue(i == 23);
        compositeByteBuf.release();
        inputStream.close();
    }
}
