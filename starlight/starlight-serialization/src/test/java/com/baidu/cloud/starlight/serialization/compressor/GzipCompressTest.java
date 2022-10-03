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
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Created by liuruisen on 2020/9/10.
 */
public class GzipCompressTest {

    private GzipCompress gzipCompress = new GzipCompress();
    private byte[] origin = "Hello".getBytes();

    @Test
    public void compress() {
        byte[] compress = gzipCompress.compress(origin);
        Assert.assertFalse(Arrays.equals(origin, compress));
    }

    @Test
    public void compressExp() {
        // input null
        byte[] compress = gzipCompress.compress(null);
        Assert.assertTrue(compress == null);

        byte[] compress2 = gzipCompress.compress(new byte[0]);
        Assert.assertTrue(compress2.length == 0);

    }

    @Test
    public void decompress() {
        byte[] compress = gzipCompress.compress(origin);
        byte[] bytes = gzipCompress.decompress(compress);
        Assert.assertTrue(Arrays.equals(bytes, origin));
    }

    @Test
    public void decompressExp() {
        // decompress null
        byte[] bytes = gzipCompress.decompress(null);
        Assert.assertTrue(bytes == null);

        // length 0
        byte[] bytes2 = gzipCompress.decompress(new byte[0]);
        Assert.assertTrue(bytes2.length == 0);

        byte[] byte3 = new byte[10];
        System.arraycopy("123".getBytes(), 0, byte3, 0, "123".getBytes().length);
        try {
            gzipCompress.decompress(byte3);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode() == CodecException.DECOMPRESS_EXCEPTION);
        }
    }

}