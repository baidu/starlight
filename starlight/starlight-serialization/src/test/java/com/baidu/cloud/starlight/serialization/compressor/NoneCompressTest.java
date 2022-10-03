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

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Created by liuruisen on 2020/9/10.
 */
public class NoneCompressTest {

    private byte[] input = "Hello".getBytes();
    private NoneCompress noneCompress = new NoneCompress();

    @Test
    public void compress() {
        byte[] compress = noneCompress.compress(input);
        Assert.assertTrue(Arrays.equals(compress, input));
    }

    @Test
    public void decompress() {
        byte[] unCompress = noneCompress.decompress(input);
        Assert.assertTrue(Arrays.equals(input, unCompress));
    }
}