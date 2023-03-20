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

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import javax.servlet.WriteListener;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Created by liuruisen on 2020/10/19.
 */
public class ByteBufServletOutputStreamTest {

    @Test
    public void isReady() throws IOException {
        ByteBufServletOutputStream outputStream =
            new ByteBufServletOutputStream(new ByteBufOutputStream(ByteBufAllocator.DEFAULT.buffer()));

        assertFalse(outputStream.isReady());

        outputStream.write(1);

        assertTrue(outputStream.isReady());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void setWriteListener() {
        ByteBufServletOutputStream outputStream =
            new ByteBufServletOutputStream(new ByteBufOutputStream(Unpooled.wrappedBuffer("Test".getBytes())));
        outputStream.setWriteListener(new WriteListener() {
            @Override
            public void onWritePossible() throws IOException {

            }

            @Override
            public void onError(Throwable throwable) {

            }
        });
    }

    @Test
    public void write() throws IOException {
        ByteBufServletOutputStream outputStream =
            new ByteBufServletOutputStream(new ByteBufOutputStream(ByteBufAllocator.DEFAULT.buffer()));

        assertFalse(outputStream.isReady());

        outputStream.write(1);

        assertTrue(outputStream.getBufferSize() > 0);
    }

    @Test
    public void resetBuffer() throws IOException {
        ByteBufServletOutputStream outputStream =
            new ByteBufServletOutputStream(new ByteBufOutputStream(ByteBufAllocator.DEFAULT.buffer()));
        assertFalse(outputStream.isReady());
        outputStream.write(1);
        assertTrue(outputStream.isReady());
        outputStream.resetBuffer();
        assertFalse(outputStream.isReady());
    }

    @Test
    public void getBufferSize() throws IOException {
        ByteBufServletOutputStream outputStream =
            new ByteBufServletOutputStream(new ByteBufOutputStream(ByteBufAllocator.DEFAULT.buffer()));
        assertEquals(0, outputStream.getBufferSize());
        outputStream.write(1);
        assertEquals(1, outputStream.getBufferSize());
    }
}