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
 
package com.baidu.cloud.starlight.transport.protocol.test;

import com.baidu.cloud.starlight.api.exception.CodecException;
import com.baidu.cloud.starlight.api.model.AbstractMsgBase;
import com.baidu.cloud.starlight.api.model.MsgBase;
import com.baidu.cloud.starlight.api.transport.buffer.DynamicCompositeByteBuf;
import com.baidu.cloud.starlight.api.protocol.ProtocolDecoder;
import io.netty.buffer.ByteBuf;

import java.util.Arrays;

/**
 * Created by liuruisen on 2020/3/20.
 */
public class TestDecoder implements ProtocolDecoder {
    @Override
    public MsgBase decode(DynamicCompositeByteBuf input) throws CodecException {
        // not enough data
        if (input.readableBytes() < ATestProtocol.FIXED_LEN) { // not match
            throw new CodecException(CodecException.PROTOCOL_INSUFFICIENT_DATA_EXCEPTION,
                "Too little data to parse using Brpc"); // wait and retry
        }
        ByteBuf fixHeaderBuf = input.retainedSlice(ATestProtocol.FIXED_LEN);
        try {
            byte[] magic = new byte[4];
            fixHeaderBuf.readBytes(magic);
            if (!Arrays.equals(magic, ATestProtocol.MAGIC_HEAD)) { // not match
                throw new CodecException(CodecException.PROTOCOL_DECODE_NOTMATCH_EXCEPTION,
                    "Magic num dose not match Brpc");
            }
            input.skipBytes(ATestProtocol.FIXED_LEN);
            return new AbstractMsgBase() {};
        } finally {
            if (fixHeaderBuf != null) {
                fixHeaderBuf.release();
            }
        }
    }

    @Override
    public void decodeBody(MsgBase msgBase) throws CodecException {}
}