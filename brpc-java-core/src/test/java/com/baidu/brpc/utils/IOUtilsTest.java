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

package com.baidu.brpc.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

public class IOUtilsTest {

    @Test
    public void testByteBufToString() throws IOException {
        ByteBuf byteBuf = Unpooled.buffer(16);
        byteBuf.writeInt(123);
        byteBuf.writeInt(456);
        ByteBufInputStream inputStream = new ByteBufInputStream(byteBuf);
        byte[] bytes = IOUtils.readInputStream(inputStream);
        Assert.assertEquals(8, bytes.length);

        ByteBuffer buf = ByteBuffer.wrap(bytes);
        Assert.assertEquals(123, buf.getInt(0));
        Assert.assertEquals(456, buf.getInt(4));
    }
}
