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
 
package com.baidu.cloud.starlight.api.protocol;

import com.baidu.cloud.starlight.api.exception.CodecException;
import com.baidu.cloud.starlight.api.model.MsgBase;
import com.baidu.cloud.starlight.api.transport.buffer.DynamicCompositeByteBuf;

/**
 * Created by liuruisen on 2019/12/4.
 */
public interface ProtocolDecoder {

    /**
     * Decode byte of the Protocol to MsgBase. Parsing protocol only, not parsing body. Execute in io thread. 1.
     * Determine the data type: Request or Response 2. Decode the byte to MsgBase 3. Throw CodecException
     *
     * @param input
     * @return
     * @throws CodecException
     */
    MsgBase decode(DynamicCompositeByteBuf input) throws CodecException;

    /**
     * Decode binary body into java params. Decode from MsgBase and save it.
     *
     * @param msgBase
     * @throws CodecException
     */
    void decodeBody(MsgBase msgBase) throws CodecException;
}
