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

import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.Iterator;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;

/**
 * dynamically composite multi {@link ByteBuf}, and it can be read just like a normal {@link ByteBuf}.
 * it can be constructed by one or many readable {@link ByteBuf},
 * reference count of these buffers is not retained, and are managed by {@link DynamicCompositeByteBuf}.
 * when dynamic composite byte buffer release, it will release every sub buffers.
 * when the readable size of a sub buffer is zero, it will be removed from dynamic composite byte buffer,
 * and will be release.
 * Currently, DynamicCompositeByteBuf does not support to write bytes,
 * because netty {@link ByteBuf} has satisfied already.
 */
public class DynamicCompositeByteBuf {
    private ArrayDeque<ByteBuf> buffers;
    private int readableBytes;

    public DynamicCompositeByteBuf() {
        this.buffers = new ArrayDeque<ByteBuf>(1);
        this.readableBytes = 0;
    }

    public DynamicCompositeByteBuf(int capacity) {
        this.buffers = new ArrayDeque<ByteBuf>(capacity);
        this.readableBytes = 0;
    }

    /**
     * construct a new {@link DynamicCompositeByteBuf} with netty {@link ByteBuf}.
     * @param buf input buf, readable size should be greater than zero.
     */
    public DynamicCompositeByteBuf(ByteBuf buf) {
        buffers = new ArrayDeque<ByteBuf>(1);
        buffers.addLast(buf);
        readableBytes += buf.readableBytes();
    }

    /**
     * construct a new {@link DynamicCompositeByteBuf} with netty {@link ByteBuf} array.
     * @param bufs byte buffer array, readable size of each element should be greater than zero.
     * @param offset offset at array
     * @param len length for create the new {@link DynamicCompositeByteBuf}
     */
    public DynamicCompositeByteBuf(ByteBuf[] bufs, int offset, int len) {
        buffers = new ArrayDeque<ByteBuf>(len);
        readableBytes = 0;
        for (int i = offset; i < offset + len; i++) {
            buffers.addLast(bufs[i]);
            readableBytes += bufs[i].readableBytes();
        }
    }

    public int readableBytes() {
        return readableBytes;
    }

    public boolean isEmpty() {
        return readableBytes == 0;
    }

    /**
     * @return whether this {@link DynamicCompositeByteBuf} has a backing array
     */
    public boolean hasArray() {
        return buffers.size() == 1 && buffers.peekFirst().hasArray();
    }

    /**
     * it not check hasArray for performance
     * @return the backing array if it exists. {@code null} otherwise.
     */
    public byte[] array() throws UnsupportedOperationException {
        if (hasArray()) {
            return buffers.peekFirst().array();
        }
        throw new UnsupportedOperationException();
    }

    /**
     * it not check hasArray for performance
     * @return The offset within this buffer's array of the first element
     *          of the buffer
     */
    public int arrayOffset() throws UnsupportedOperationException {
        if (hasArray()) {
            return buffers.peekFirst().arrayOffset();
        }
        throw new UnsupportedOperationException();
    }

    public int readerIndex() {
        if (hasArray()) {
            return buffers.peekFirst().readerIndex();
        }
        throw new UnsupportedOperationException();
    }

    public boolean isReadable() {
        return readableBytes > 0;
    }

    /**
     * convert {@link DynamicCompositeByteBuf} to netty {@link ByteBuf},
     * the reference count of its underlying buffers are not increased.
     * @return netty ByteBuf
     */
    public ByteBuf nettyByteBuf() {
        if (readableBytes == 0) {
            return Unpooled.EMPTY_BUFFER;
        }
        int size = buffers.size();
        if (size == 1) {
            return buffers.pop();
        }
        return new CompositeByteBuf(UnpooledByteBufAllocator.DEFAULT, false,
                size, buffers.toArray(new ByteBuf[0]));
    }

    /**
     * add netty {@link ByteBuf} to {@link DynamicCompositeByteBuf}.
     * the reference count of netty byte buffer will be managed by {@link DynamicCompositeByteBuf}.
     * @param buffer netty byte buffer
     */
    public void addBuffer(ByteBuf buffer) {
        if (buffer != null) {
            int bufLen = buffer.readableBytes();
            if (bufLen > 0) {
                buffers.add(buffer);
                readableBytes += bufLen;
            }
        }
    }

    /**
     * Returns a new {@link CompositeByteBuf} which slice the first {@code length} of this
     * composite byte buffer and increases the {@code readerIndex} by the size
     * of the new slice (= {@code length}).
     *
     * @param length the size of the new composite byte buffer
     *
     * @return the newly created composite byte buffer
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.readableBytes}
     */
    public ByteBuf readRetainedSlice(int length) {
        if (length > readableBytes) {
            throw new IndexOutOfBoundsException("length > readableBytes");
        }
        if (length <= 0) {
            return Unpooled.buffer(0);
        }

        ByteBuf first = buffers.peek();
        int firstLen = first.readableBytes();
        if (length == firstLen) {
            readableBytes -= length;
            return buffers.removeFirst();
        } else if (length < firstLen) {
            ByteBuf newBuf = first.readRetainedSlice(length);
            readableBytes -= length;
            return newBuf;
        } else {
            int capacity = 2;
            ByteBuf[] byteBufs = new ByteBuf[capacity];
            int i = 0;
            while (length > 0 && readableBytes > 0) {
                ByteBuf newBuf;
                if (firstLen > length) {
                    newBuf = first.readRetainedSlice(length);
                    readableBytes -= length;
                    length = 0;
                } else {
                    newBuf = first;
                    readableBytes -= firstLen;
                    length -= firstLen;
                    buffers.pop();
                }
                if (i == capacity) {
                    int newCapacity = capacity * 2;
                    ByteBuf[] newByteBufs = new ByteBuf[newCapacity];
                    System.arraycopy(byteBufs, 0, newByteBufs, 0, capacity);
                    byteBufs = newByteBufs;
                    capacity = newCapacity;
                }
                byteBufs[i++] = newBuf;
                first = buffers.peek();
                if (first == null) {
                    break;
                }
                firstLen = first.readableBytes();
            }
            if (i == capacity) {
                int maxComponentNum = (i > 2) ? i : 2;
                return new CompositeByteBuf(UnpooledByteBufAllocator.DEFAULT, true, maxComponentNum, byteBufs);
            } else {
                ByteBuf[] outBufs = new ByteBuf[i];
                System.arraycopy(byteBufs, 0, outBufs, 0, i);
                int maxComponentNum = (i > 2) ? i : 2;
                return new CompositeByteBuf(UnpooledByteBufAllocator.DEFAULT, true, maxComponentNum, outBufs);
            }
        }
    }

    /**
     * Returns a new {@link CompositeByteBuf} which slice the first {@code length} of this
     * composite byte buffer while they maintain separate indexes and marks.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @param length the size of the new composite byte buffer
     *
     * @return the newly created composite byte buffer
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.readableBytes}
     */
    public ByteBuf retainedSlice(int length) {
        if (length > readableBytes) {
            throw new IndexOutOfBoundsException("length > readableBytes");
        }
        if (length <= 0) {
            return Unpooled.buffer(0);
        }

        ByteBuf first = buffers.peek();
        int firstLen = first.readableBytes();
        if (length <= firstLen) {
            ByteBuf newBuf = first.retainedSlice(first.readerIndex(), length);
            return newBuf;
        } else {
            int capacity = 2;
            ByteBuf[] byteBufs = new ByteBuf[capacity];
            int i = 0;
            int offset = 0;
            ByteBuf newBuf;
            Iterator<ByteBuf> iterator = buffers.iterator();
            while (offset < length && iterator.hasNext()) {
                ByteBuf next = iterator.next();
                int nextLen = next.readableBytes();
                if (nextLen <= length - offset) {
                    newBuf = next.retainedSlice();
                    offset += nextLen;
                } else {
                    newBuf = next.retainedSlice(next.readerIndex(), length - offset);
                    offset = length;
                }
                if (i == capacity) {
                    int newCapacity = capacity * 2;
                    ByteBuf[] newByteBufs = new ByteBuf[newCapacity];
                    System.arraycopy(byteBufs, 0, newByteBufs, 0, i);
                    byteBufs = newByteBufs;
                    capacity = newCapacity;
                }
                byteBufs[i++] = newBuf;
            }
            if (i == capacity) {
                int maxComponentNum = (i > 2) ? i : 2;
                return new CompositeByteBuf(UnpooledByteBufAllocator.DEFAULT, true, maxComponentNum, byteBufs);
            } else {
                ByteBuf[] outBufs = new ByteBuf[i];
                System.arraycopy(byteBufs, 0, outBufs, 0, i);
                int maxComponentNum = (i > 2) ? i : 2;
                return new CompositeByteBuf(UnpooledByteBufAllocator.DEFAULT, true, maxComponentNum, outBufs);
            }
        }
    }

    public void skipBytes(int length) {
        if (length > readableBytes) {
            throw new IndexOutOfBoundsException("length > readableBytes");
        }
        if (length <= 0) {
            return;
        }
        while (length > 0 && readableBytes > 0) {
            ByteBuf first = buffers.peek();
            int firstLen = first.readableBytes();
            if (firstLen > length) {
                first.skipBytes(length);
                readableBytes -= length;
                length = 0;
            } else {
                readableBytes -= firstLen;
                length -= firstLen;
                buffers.removeFirst().release();
            }
        }
        return;
    }

    public DynamicCompositeByteBuf readBytes(byte[] dst) {
        return readBytes(dst, 0, dst.length);
    }

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the current {@code readerIndex} and increases the {@code readerIndex}
     * by the number of the transferred bytes (= {@code length}).
     *
     * @param dstIndex the first index of the destination
     * @param length   the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code dstIndex} is less than {@code 0},
     *         if {@code length} is greater than {@code this.readableBytes}, or
     *         if {@code dstIndex + length} is greater than {@code dst.length}
     */
    public DynamicCompositeByteBuf readBytes(byte[] dst, int dstIndex, int length) {
        if (dst == null) {
            throw new NullPointerException();
        }
        if (dstIndex < 0 || dstIndex >= dst.length
                || dstIndex + length > dst.length || dstIndex + length < 0) {
            throw new IndexOutOfBoundsException();
        }

        while (length > 0) {
            ByteBuf first = buffers.peek();
            int firstLen = first.readableBytes();
            if (firstLen > length) {
                first.readBytes(dst, dstIndex, length);
                readableBytes -= length;
                length = 0;
            } else {
                first.readBytes(dst, dstIndex, firstLen);
                readableBytes -= firstLen;
                dstIndex += firstLen;
                length -= firstLen;
                buffers.removeFirst().release();
            }
        }
        return this;
    }

    /**
     * Gets a byte at the current {@code readerIndex} and increases
     * the {@code readerIndex} by {@code 1} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 1}
     */
    public byte readByte() {
        checkReadableBytes0(1);
        ByteBuf buf = buffers.peek();
        byte res = buf.readByte();
        readableBytes -= 1;
        if (buf.readableBytes() <= 0) {
            buffers.removeFirst().release();
        }
        return res;
    }

    /**
     * Gets an unsigned byte at the current {@code readerIndex} and increases
     * the {@code readerIndex} by {@code 1} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 1}
     */
    public short readUnsignedByte() {
        return (short) (readByte() & 0xFF);
    }

    /**
     * Gets a 16-bit short integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 2} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 2}
     */
    public short readShort() {
        checkReadableBytes0(2);
        short res;
        ByteBuf first = buffers.peek();
        int firstLen = first.readableBytes();
        if (firstLen >= 2) {
            res = first.readShort();
            readableBytes -= 2;
            if (firstLen == 2) {
                buffers.removeFirst().release();
            }
        } else if (order() == ByteOrder.BIG_ENDIAN) {
            res = (short) ((readByte() & 0xff) << 8 | readByte() & 0xff);
        } else {
            res = (short) (readByte() & 0xff | (readByte() & 0xff) << 8);
        }
        return res;
    }

    /**
     * Gets a 16-bit short integer at the current {@code readerIndex}
     * in the Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 2} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 2}
     */
    public short readShortLE() {
        checkReadableBytes0(2);
        ByteBuf first = buffers.peek();
        int firstLen = first.readableBytes();
        if (firstLen >= 2) {
            short res = first.readShortLE();
            readableBytes -= 2;
            if (firstLen == 2) {
                buffers.removeFirst().release();
            }
            return res;
        } else if (order() == ByteOrder.BIG_ENDIAN) {
            return (short) (readByte() & 0xff | (readByte() & 0xff) << 8);
        } else {
            return (short) ((readByte() & 0xff) << 8 | readByte() & 0xff);
        }
    }

    /**
     * Gets an unsigned 16-bit short integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 2} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 2}
     */
    public int readUnsignedShort() {
        return readShort() & 0xFFFF;
    }

    /**
     * Gets an unsigned 16-bit short integer at the current {@code readerIndex}
     * in the Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 2} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 2}
     */
    public int readUnsignedShortLE() {
        return readShortLE() & 0xFFFF;
    }

    /**
     * Gets a 32-bit integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 4} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 4}
     */
    public int readInt() {
        checkReadableBytes0(4);
        ByteBuf first = buffers.peek();
        int firstLen = first.readableBytes();
        if (firstLen >= 4) {
            int res = first.readInt();
            readableBytes -= 4;
            if (firstLen == 4) {
                buffers.removeFirst().release();
            }
            return res;
        } else if (order() == ByteOrder.BIG_ENDIAN) {
            return (readShort() & 0xffff) << 16 | readShort() & 0xffff;
        } else {
            return readShort() & 0xFFFF | (readShort() & 0xFFFF) << 16;
        }
    }

    /**
     * Gets a 32-bit integer at the current {@code readerIndex}
     * in the Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 4} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 4}
     */
    public int readIntLE() {
        checkReadableBytes0(4);
        ByteBuf first = buffers.peek();
        int firstLen = first.readableBytes();
        if (firstLen >= 4) {
            int res = first.readIntLE();
            readableBytes -= 4;
            if (firstLen == 4) {
                buffers.removeFirst().release();
            }
            return res;
        } else if (order() == ByteOrder.BIG_ENDIAN) {
            return readShortLE() & 0xffff | (readShortLE() & 0xffff) << 16;
        } else {
            return (readShortLE() & 0xffff) << 16 | readShortLE() & 0xffff;
        }
    }

    /**
     * Gets a 64-bit integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 8} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 8}
     */
    public long readLong() {
        checkReadableBytes0(8);
        ByteBuf first = buffers.peek();
        int firstLen = first.readableBytes();
        if (firstLen >= 8) {
            long res = first.readLong();
            readableBytes -= 8;
            if (firstLen == 8) {
                buffers.removeFirst().release();
            }
            return res;
        } else if (order() == ByteOrder.BIG_ENDIAN) {
            return (readInt() & 0xffffffffL) << 32 | readInt() & 0xffffffffL;
        } else {
            return readInt() & 0xFFFFFFFFL | (readInt() & 0xFFFFFFFFL) << 32;
        }
    }

    /**
     * Gets a 64-bit integer at the current {@code readerIndex}
     * in the Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 8} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 8}
     */
    public long readLongLE() {
        checkReadableBytes0(8);
        ByteBuf first = buffers.peek();
        int firstLen = first.readableBytes();
        if (firstLen >= 8) {
            long res = first.readLongLE();
            readableBytes -= 8;
            if (firstLen == 8) {
                buffers.removeFirst().release();
            }
            return res;
        } else if (order() == ByteOrder.BIG_ENDIAN) {
            return readIntLE() & 0xffffffffL | (readIntLE() & 0xffffffffL) << 32;
        } else {
            return (readIntLE() & 0xffffffffL) << 32 | readIntLE() & 0xffffffffL;
        }
    }

    /**
     * Gets a 2-byte UTF-16 character at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 2} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 2}
     */
    public char readChar() {
        return (char) readShort();
    }

    /**
     * Gets a 32-bit floating point number at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 4} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 4}
     */
    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    /**
     * Gets a 32-bit floating point number at the current {@code readerIndex}
     * in Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 4} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 4}
     */
    public float readFloatLE() {
        return Float.intBitsToFloat(readIntLE());
    }

    /**
     * Gets a 64-bit floating point number at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 8} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 8}
     */
    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    /**
     * Gets a 64-bit floating point number at the current {@code readerIndex}
     * in Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 8} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 8}
     */
    public double readDoubleLE() {
        return Double.longBitsToDouble(readLongLE());
    }

    public ByteOrder order() {
        return ByteOrder.BIG_ENDIAN;
    }

    private void checkReadableBytes0(int length) {
        if (readableBytes < length) {
            throw new IndexOutOfBoundsException(String.format(
                    "length(%d) exceeds readableBytes(%d): %s",
                    length, readableBytes, this));
        }
    }

    /**
     * If the current buffer is exhausted, removes and closes it.
     */
    private void advanceBufferIfNecessary() {
        while (!buffers.isEmpty()) {
            ByteBuf buffer = buffers.peek();
            if (buffer.readableBytes() == 0) {
                buffers.remove().release();
            } else {
                break;
            }
        }
    }

    public void release() {
        readableBytes = 0;
        while (!buffers.isEmpty()) {
            buffers.remove().release();
        }
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (readableBytes == 0) {
            return sb.toString();
        }
        Iterator<ByteBuf> iterator = buffers.iterator();
        while (iterator.hasNext()) {
            ByteBuf buf = iterator.next();
            for (int i = buf.readerIndex(); i < buf.readerIndex() + buf.readableBytes(); i++) {
                sb.append(buf.getUnsignedByte(i)).append(" ");
            }
        }
        return sb.toString();
    }

}
