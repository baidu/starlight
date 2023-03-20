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
 
package com.baidu.cloud.starlight.transport.buffer;

import com.baidu.cloud.starlight.api.transport.buffer.DynamicCompositeByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by liuruisen on 2020/3/20.
 */
public class DynamicCompositeByteBufTest {

    @Test
    public void testDynamicCompositeByteBuf() {
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf();
        Assert.assertTrue(compositeByteBuf.readableBytes() == 0);
    }

    @Test
    public void testDynamicCompositeByteBuf2() {
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf(2);
        Assert.assertTrue(compositeByteBuf.readableBytes() == 0);
    }

    @Test
    public void testDynamicCompositeByteBuf3() {
        ByteBuf byteBuf1 = Unpooled.wrappedBuffer("hello".getBytes());
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf(byteBuf1);
        Assert.assertTrue(compositeByteBuf.readableBytes() == byteBuf1.readableBytes());
        Assert.assertTrue(byteBuf1.refCnt() == 1);
    }

    @Test
    public void testDynamicCompositeByteBuf4() {
        ByteBuf buf1 = Unpooled.wrappedBuffer("hello1".getBytes());
        ByteBuf buf2 = Unpooled.wrappedBuffer("hello222".getBytes());
        ByteBuf buf3 = Unpooled.wrappedBuffer("hello33333".getBytes());
        ByteBuf[] bufs = new ByteBuf[] {buf1, buf2, buf3};
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf(bufs, 1, 2);
        Assert.assertTrue(compositeByteBuf.readableBytes() == buf2.readableBytes() + buf3.readableBytes());
    }

    @Test
    public void testReadableBytes() {
        ByteBuf byteBuf1 = Unpooled.wrappedBuffer("hello".getBytes());
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf(byteBuf1);
        Assert.assertTrue(compositeByteBuf.readableBytes() == byteBuf1.readableBytes());
    }

    @Test
    public void testIsEmpty() {
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf();
        Assert.assertTrue(compositeByteBuf.isEmpty() == true);

        ByteBuf buf1 = Unpooled.wrappedBuffer("hello1".getBytes());
        DynamicCompositeByteBuf compositeByteBuf2 = new DynamicCompositeByteBuf(buf1);
        Assert.assertTrue(compositeByteBuf2.isEmpty() == false);
    }

    @Test
    public void testHasArray() {
        ByteBuf buf1 = Unpooled.wrappedBuffer("hello1".getBytes());
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf(buf1);
        Assert.assertTrue(compositeByteBuf.hasArray() == true);

        ByteBuf buf2 = Unpooled.wrappedBuffer(buf1);
        compositeByteBuf.addBuffer(buf2);
        Assert.assertTrue(compositeByteBuf.hasArray() == false);
    }

    @Test
    public void testArray() {
        ByteBuf buf1 = Unpooled.wrappedBuffer("hello1".getBytes());
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf(buf1);
        Assert.assertTrue(buf1.array() == compositeByteBuf.array());
    }

    @Test
    public void testArrayOffset() {
        ByteBuf buf1 = Unpooled.wrappedBuffer("hello1".getBytes());
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf(buf1);
        Assert.assertTrue(buf1.arrayOffset() == compositeByteBuf.arrayOffset());
    }

    @Test
    public void testReaderIndex() {
        ByteBuf buf1 = Unpooled.wrappedBuffer("hello1".getBytes());
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf(buf1);
        Assert.assertTrue(buf1.readerIndex() == compositeByteBuf.readerIndex());
    }

    @Test
    public void testNettyBuf() {
        ByteBuf buf1 = Unpooled.wrappedBuffer("hello1".getBytes());
        ByteBuf buf2 = Unpooled.wrappedBuffer("hello2".getBytes());
        ByteBuf[] bufs = new ByteBuf[] {buf1, buf2};
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf(bufs, 0, 2);
        ByteBuf byteBuf = compositeByteBuf.nettyByteBuf();
        Assert.assertTrue(byteBuf.readableBytes() == buf1.readableBytes() + buf2.readableBytes());
    }

    @Test
    public void testAddBuffer() {
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf();
        Assert.assertTrue(compositeByteBuf.readableBytes() == 0);
        ByteBuf byteBuf1 = Unpooled.wrappedBuffer("hello".getBytes());
        Assert.assertTrue(byteBuf1.refCnt() == 1);
        compositeByteBuf.addBuffer(byteBuf1);
        Assert.assertTrue(compositeByteBuf.readableBytes() == byteBuf1.readableBytes());
        Assert.assertTrue(byteBuf1.refCnt() == 1);

        ByteBuf byteBuf2 = Unpooled.wrappedBuffer("world".getBytes());
        compositeByteBuf.addBuffer(byteBuf2);
        Assert.assertTrue(compositeByteBuf.readableBytes() == byteBuf1.readableBytes() + byteBuf2.readableBytes());

        compositeByteBuf.release();
        Assert.assertTrue(byteBuf1.refCnt() == 0);
    }

    @Test
    public void testReadRetainedSlice() {
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf();
        ByteBuf buf = Unpooled.buffer(8);
        buf.writeInt(1);
        buf.writeInt(2);
        compositeByteBuf.addBuffer(buf);
        ByteBuf compositeByteBuf2 = compositeByteBuf.readRetainedSlice(4);
        Assert.assertTrue(compositeByteBuf.readableBytes() == 4);
        Assert.assertTrue(compositeByteBuf.readInt() == 2);
        Assert.assertTrue(compositeByteBuf2.readableBytes() == 4);
        Assert.assertTrue(compositeByteBuf2.readInt() == 1);
        compositeByteBuf2.release();
        Assert.assertTrue(buf.refCnt() == 0);
    }

    @Test
    public void testRetainedSlice() {
        ByteBuf buf1 = Unpooled.buffer(8);
        buf1.writeInt(1);
        buf1.writeInt(2);

        ByteBuf buf2 = Unpooled.buffer(16);
        buf2.writeLong(10);
        buf2.writeLong(20);

        ByteBuf[] bufs = new ByteBuf[2];
        bufs[0] = buf1;
        bufs[1] = buf2;
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf(bufs, 0, 2);
        Assert.assertTrue(compositeByteBuf.readableBytes() == 24);

        ByteBuf compositeByteBuf2 = compositeByteBuf.retainedSlice(12);
        Assert.assertTrue(compositeByteBuf.readableBytes() == 24);
        Assert.assertTrue(compositeByteBuf2.readInt() == 1);
        Assert.assertTrue(buf1.refCnt() == 2);
        Assert.assertTrue(buf2.refCnt() == 2);
        compositeByteBuf2.release();
        Assert.assertTrue(buf1.refCnt() == 1);
        Assert.assertTrue(buf2.refCnt() == 1);
    }

    @Test
    public void testSkipBytes() {
        byte[] bytes1 = "hello".getBytes();
        byte[] bytes2 = "hello".getBytes();
        ByteBuf buf1 = Unpooled.wrappedBuffer(bytes1);
        ByteBuf buf2 = Unpooled.wrappedBuffer(bytes2);
        ByteBuf[] bufs = new ByteBuf[] {buf1, buf2};
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf(bufs, 0, 2);
        compositeByteBuf.skipBytes(bytes1.length);
        Assert.assertTrue(compositeByteBuf.readableBytes() == bytes2.length);
        Assert.assertTrue(compositeByteBuf.hasArray() == true);
        Assert.assertTrue(buf1.refCnt() == 0);
        Assert.assertTrue(buf2.refCnt() == 1);
    }

    @Test
    public void testReadBytes() {
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf();
        ByteBuf byteBuf1 = Unpooled.wrappedBuffer("hello".getBytes());
        compositeByteBuf.addBuffer(byteBuf1);
        byte[] bytes = new byte[byteBuf1.readableBytes()];
        compositeByteBuf.readBytes(bytes);
        String msg = new String(bytes);
        Assert.assertTrue(msg.equals("hello"));
    }

    @Test
    public void testReadByte() {
        byte[] bytes = new byte[] {12, 23, 34, 45, 56};
        ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf(buf);
        byte b = compositeByteBuf.readByte();
        Assert.assertTrue(b == 12);
        Assert.assertTrue(compositeByteBuf.readableBytes() == 4);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadByteThrowException() {
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf();
        compositeByteBuf.readByte();
    }

    @Test
    public void testReadUnsignedByte() {
        byte[] bytes = new byte[] {-1, 23, 34, 45, 56};
        ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf(buf);
        short s = compositeByteBuf.readUnsignedByte();
        Assert.assertTrue(s == 255);
    }

    @Test
    public void testReadShort() {
        byte[] bytes = new byte[] {1, 2, 34, 45, 56};
        ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf(buf);
        short s = compositeByteBuf.readShort();
        Assert.assertTrue(s == 258);
    }

    @Test
    public void testReadShortLE() {
        byte[] bytes = new byte[] {1, 2, 34, 45, 56};
        ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf(buf);
        short s = compositeByteBuf.readShortLE();
        Assert.assertTrue(s == 513);
    }

    @Test
    public void testReadUnsignedShort() {
        ByteBuf buf = Unpooled.buffer(8);
        buf.writeShort(65536);
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf(buf);
        int s = compositeByteBuf.readUnsignedShort();
        Assert.assertTrue(s == 0);
    }

    @Test
    public void testReadInt() {
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf();
        ByteBuf buf = Unpooled.buffer(8);
        buf.writeInt(1);
        buf.writeInt(2);
        compositeByteBuf.addBuffer(buf);
        Assert.assertTrue(compositeByteBuf.readInt() == 1);
        Assert.assertTrue(compositeByteBuf.readInt() == 2);
    }

    @Test
    public void testReadIntLE() {
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf();
        ByteBuf buf = Unpooled.buffer(8);
        buf.writeIntLE(123);
        compositeByteBuf.addBuffer(buf);
        Assert.assertTrue(compositeByteBuf.readIntLE() == 123);
    }

    @Test
    public void testReadLong() {
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf();
        ByteBuf buf = Unpooled.buffer(8);
        buf.writeLong(234L);
        compositeByteBuf.addBuffer(buf);
        Assert.assertTrue(compositeByteBuf.readLong() == 234L);
    }

    @Test
    public void testReadLongLE() {
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf();
        ByteBuf buf = Unpooled.buffer(8);
        buf.writeLongLE(234L);
        compositeByteBuf.addBuffer(buf);
        Assert.assertTrue(compositeByteBuf.readLongLE() == 234L);
    }

    @Test
    public void testRelease() {
        byte[] bytes1 = "hello".getBytes();
        byte[] bytes2 = "hello".getBytes();
        ByteBuf buf1 = Unpooled.wrappedBuffer(bytes1);
        ByteBuf buf2 = Unpooled.wrappedBuffer(bytes2);
        ByteBuf[] bufs = new ByteBuf[] {buf1, buf2};
        DynamicCompositeByteBuf compositeByteBuf = new DynamicCompositeByteBuf(bufs, 0, 2);
        Assert.assertTrue(buf1.refCnt() == 1);
        Assert.assertTrue(buf2.refCnt() == 1);
        compositeByteBuf.release();
        Assert.assertTrue(buf1.refCnt() == 0);
        Assert.assertTrue(buf2.refCnt() == 0);
    }

}