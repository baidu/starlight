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
import io.netty.buffer.Unpooled;
import javax.servlet.ReadListener;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by liuruisen on 2020/6/30.
 */
public class ByteBufServletInputStreamTest {

    private ByteBufServletInputStream byteBufInputStream;

    private static final int LENGTH = "Test".getBytes().length;
    private static final byte[] BYTES = "Test".getBytes();

    @Before
    public void setup() {
        byteBufInputStream = new ByteBufServletInputStream(new ByteBufInputStream(Unpooled.wrappedBuffer(BYTES)));
    }

    @Test
    public void isFinished() throws IOException {
        Assert.assertFalse(byteBufInputStream.isFinished());
        byte[] bytes1 = new byte[LENGTH];
        byteBufInputStream.read(bytes1);
        Assert.assertTrue(byteBufInputStream.isFinished());
    }

    @Test
    public void isReady() {
        Assert.assertTrue(byteBufInputStream.isReady());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void setReadListener() {
        byteBufInputStream.setReadListener(new ReadListener() {
            @Override
            public void onDataAvailable() throws IOException {

            }

            @Override
            public void onAllDataRead() throws IOException {

            }

            @Override
            public void onError(Throwable t) {

            }
        });
    }

    @Test
    public void read() throws IOException {
        int byteValue = byteBufInputStream.read(); // read the next byte

        Assert.assertTrue((int) BYTES[0] == byteValue);

        Assert.assertEquals(byteBufInputStream.available(), LENGTH - 1);
    }

    @Test
    public void readBytes() throws IOException {
        byte[] bytes1 = new byte[LENGTH];
        byteBufInputStream.read(bytes1);
        Assert.assertEquals(new String(bytes1), "Test");
        Assert.assertEquals(byteBufInputStream.available(), 0);
    }

    @Test
    public void readBytesLen() throws IOException {
        byte[] bytes1 = new byte[LENGTH];
        byteBufInputStream.read(bytes1, 0, 1);
        Assert.assertEquals(bytes1[0], BYTES[0]);
    }

    @Test
    public void skip() throws IOException {
        byteBufInputStream.skip(1);
        Assert.assertEquals(byteBufInputStream.available(), LENGTH - 1);
    }

    @Test
    public void available() throws IOException {
        Assert.assertEquals(byteBufInputStream.available(), LENGTH);
    }

    @Test
    public void close() throws IOException {
        byteBufInputStream.close();
        // Assert.assertFalse(byteBufInputStream.isReady());
    }

    @Test
    public void markSupported() {
        Assert.assertTrue(byteBufInputStream.markSupported());
    }
}