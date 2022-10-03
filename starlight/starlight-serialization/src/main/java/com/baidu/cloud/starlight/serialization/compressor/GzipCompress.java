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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * GZip Compressor SPI name: gzip
 */
public class GzipCompress implements Compress {

    private static final Integer UNCOMPRESS_BYTE_SIZE = 256;

    @Override
    public byte[] compress(byte[] inputByte) throws CodecException {
        if (inputByte == null || inputByte.length == 0) {
            return inputByte;
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();) {
            GZIPOutputStream gzipStream = new GZIPOutputStream(outputStream);
            gzipStream.write(inputByte);
            gzipStream.close();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new CodecException(CodecException.COMPRESS_EXCEPTION, "Gzip compress error: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] decompress(byte[] inputByte) throws CodecException {
        if (inputByte == null || inputByte.length == 0) {
            return inputByte;
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(inputByte)) {
            GZIPInputStream unGzip = new GZIPInputStream(in);
            byte[] buffer = new byte[UNCOMPRESS_BYTE_SIZE];
            int n;
            while ((n = unGzip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
            unGzip.close();
            return out.toByteArray();
        } catch (IOException e) {
            throw new CodecException(CodecException.DECOMPRESS_EXCEPTION, "Gzip decompress error: " + e.getMessage(),
                e);
        }
    }
}