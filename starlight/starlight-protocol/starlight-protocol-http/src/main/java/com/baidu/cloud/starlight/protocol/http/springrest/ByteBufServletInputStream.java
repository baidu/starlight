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
 
package com.baidu.cloud.starlight.protocol.http.springrest;

import io.netty.buffer.ByteBufInputStream;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.IOException;

/**
 * Wrapper {@link ByteBufInputStream} as ServletInputStream. Used when converting
 * {@link io.netty.handler.codec.http.FullHttpRequest} to {@link javax.servlet.ServletRequest}.
 * 
 * @see NettyServletRequestAdaptor#getInputStream() Created by liuruisen on 2020/6/8.
 */
public class ByteBufServletInputStream extends ServletInputStream {

    private final ByteBufInputStream byteBufInputStream;

    public ByteBufServletInputStream(ByteBufInputStream byteBufInputStream) {
        this.byteBufInputStream = byteBufInputStream;
    }

    @Override
    public boolean isFinished() {
        try {
            return byteBufInputStream.available() == 0;
        } catch (IOException e) {
            return true;
        }
    }

    @Override
    public boolean isReady() {
        try {
            return available() > 0;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        throw new UnsupportedOperationException("ByteBufServletInputStream not support setListener");
    }

    @Override
    public int read() throws IOException {
        return byteBufInputStream.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return byteBufInputStream.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return byteBufInputStream.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return byteBufInputStream.skip(n);
    }

    @Override
    public int available() throws IOException {
        return byteBufInputStream.available();
    }

    @Override
    public void close() throws IOException {
        byteBufInputStream.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        byteBufInputStream.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        byteBufInputStream.reset();
    }

    @Override
    public boolean markSupported() {
        return byteBufInputStream.markSupported();
    }
}
