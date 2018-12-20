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

import java.io.InputStream;
import java.io.IOException;
import java.io.EOFException;
import java.io.DataInput;
import java.io.DataInputStream;

/**
 * An {@link InputStream} which reads data from a {@link DynamicCompositeByteBuf}.
 * <p>
 * A read operation against this stream will occur at the {@code readerIndex}
 * of its underlying buffer and the {@code readerIndex} will increase during
 * the read operation.  Please note that it only reads up to the number of
 * readable bytes of its underlying buffer.
 * <p>
 * This stream implements {@link DataInput} for your convenience.
 *
 */
public class DynamicCompositeByteBufInputStream extends InputStream implements DataInput {
    private DynamicCompositeByteBuf buffer;
    private boolean closed;
    private boolean releaseOnClose;

    /**
     * Creates a new stream which reads data from the specified {@code buffer}
     * @param compositeByteBuf The buffer which provides the content for this {@link InputStream}.
     */
    public DynamicCompositeByteBufInputStream(DynamicCompositeByteBuf compositeByteBuf) {
        this(compositeByteBuf, false);
    }

    /**
     * Creates a new stream which reads data from the specified {@code buffer}
     * @param buffer The buffer which provides the content for this {@link InputStream}.
     * @param releaseOnClose {@code true} means that when {@link #close()} is called
     *                                   then {@link DynamicCompositeByteBuf#release()}
     *                                   will be called on {@code buffer}.
     */
    public DynamicCompositeByteBufInputStream(DynamicCompositeByteBuf buffer, boolean releaseOnClose) {
        if (buffer == null) {
            throw new NullPointerException("buffer");
        }

        this.releaseOnClose = releaseOnClose;
        this.buffer = buffer;
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            // The Closable interface says "If the stream is already closed then invoking this method has no effect."
            if (releaseOnClose && !closed) {
                closed = true;
                buffer.release();
            }
        }
    }

    @Override
    public int available() throws IOException {
        return buffer.readableBytes();
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public int read() throws IOException {
        if (!buffer.isReadable()) {
            return -1;
        }
        return buffer.readByte() & 0xff;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int available = available();
        if (available == 0) {
            return -1;
        }

        len = Math.min(available, len);
        buffer.readBytes(b, off, len);
        return len;
    }

    @Override
    public long skip(long n) throws IOException {
        if (n > Integer.MAX_VALUE) {
            return skipBytes(Integer.MAX_VALUE);
        } else {
            return skipBytes((int) n);
        }
    }

    public int skipBytes(int n) throws IOException {
        int nBytes = Math.min(available(), n);
        buffer.skipBytes(nBytes);
        return nBytes;
    }

    @Override
    public boolean readBoolean() throws IOException {
        checkAvailable(1);
        return read() != 0;
    }

    @Override
    public byte readByte() throws IOException {
        if (!buffer.isReadable()) {
            throw new EOFException();
        }
        return buffer.readByte();
    }

    @Override
    public char readChar() throws IOException {
        return (char) readShort();
    }

    @Override
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    @Override
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    @Override
    public void readFully(byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {
        checkAvailable(len);
        buffer.readBytes(b, off, len);
    }

    @Override
    public int readInt() throws IOException {
        checkAvailable(4);
        return buffer.readInt();
    }

    private final StringBuilder lineBuf = new StringBuilder();

    @Override
    public String readLine() throws IOException {
        lineBuf.setLength(0);

    loop:
        while (true) {
            if (!buffer.isReadable()) {
                return lineBuf.length() > 0 ? lineBuf.toString() : null;
            }

            int c = buffer.readUnsignedByte();
            switch (c) {
                case '\n':
                    break loop;

                case '\r':
                    if (buffer.isReadable() && (char) buffer.readUnsignedByte() == '\n') {
                        buffer.skipBytes(1);
                    }
                    break loop;

                default:
                    lineBuf.append((char) c);
            }
        }

        return lineBuf.toString();
    }

    @Override
    public long readLong() throws IOException {
        checkAvailable(8);
        return buffer.readLong();
    }

    @Override
    public short readShort() throws IOException {
        checkAvailable(2);
        return buffer.readShort();
    }

    @Override
    public String readUTF() throws IOException {
        return DataInputStream.readUTF(this);
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return readByte() & 0xff;
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return readShort() & 0xffff;
    }

    private void checkAvailable(int fieldSize) throws IOException {
        if (fieldSize < 0) {
            throw new IndexOutOfBoundsException("fieldSize cannot be a negative number");
        }
        if (fieldSize > available()) {
            throw new EOFException("fieldSize is too long! Length is " + fieldSize
                    + ", but maximum is " + available());
        }
    }

}
