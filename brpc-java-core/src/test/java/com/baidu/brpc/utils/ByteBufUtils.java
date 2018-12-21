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

public class ByteBufUtils {
    public static String byteBufToString(ByteBuf buf) {
        StringBuilder sb = new StringBuilder();
        if (buf.readableBytes() == 0) {
            return sb.toString();
        }
        for (int i = buf.readerIndex(); i < buf.readerIndex() + buf.readableBytes(); i++) {
            sb.append(buf.getUnsignedByte(i)).append(" ");
        }
        return sb.toString();
    }
}
