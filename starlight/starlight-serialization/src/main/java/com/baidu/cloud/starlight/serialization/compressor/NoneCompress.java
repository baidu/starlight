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
 
package com.baidu.cloud.starlight.serialization.compressor;

import com.baidu.cloud.starlight.api.exception.CodecException;
import com.baidu.cloud.starlight.api.serialization.compressor.Compress;

/**
 * None compressor Return as is. Created by liuruisen on 2019-05-10.
 */
public class NoneCompress implements Compress {

    @Override
    public byte[] compress(byte[] inputBytes) throws CodecException {
        return inputBytes;
    }

    @Override
    public byte[] decompress(byte[] outputBytes) throws CodecException {
        return outputBytes;
    }
}
