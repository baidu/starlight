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

import io.netty.buffer.ByteBufOutputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

import java.io.IOException;

/**
 * Netty implement of ServletOutputStream Created by liuruisen on 2020/10/16.
 */
public class ByteBufServletOutputStream extends ServletOutputStream {

    private ByteBufOutputStream byteBufOutputStream;

    public ByteBufServletOutputStream(ByteBufOutputStream byteBufOutputStream) {
        this.byteBufOutputStream = byteBufOutputStream;
    }

    @Override
    public boolean isReady() {
        return byteBufOutputStream.writtenBytes() > 0;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        // FIXME 寻找更优的解法
        throw new UnsupportedOperationException("ByteBufServletOutputStream not support setListener");
    }

    @Override
    public void write(int b) throws IOException {
        // FIXME 实现过于简单，寻找最优解，最好能结合WriteListener
        byteBufOutputStream.write(b);
    }

    public void resetBuffer() {
        this.byteBufOutputStream.buffer().clear();
    }

    public int getBufferSize() {
        return this.byteBufOutputStream.buffer().readableBytes();
    }
}
