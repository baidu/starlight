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
 
package com.baidu.cloud.starlight.api.utils;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by liuruisen on 2020/3/9.
 */
public class ByteArrayUtilsTest {

    @Test
    public void byteMerger() {
        byte[] byte1 = "123".getBytes();
        byte[] byte2 = "456".getBytes();
        byte[] result = ByteArrayUtils.byteMerger(byte1, byte2);
        Assert.assertTrue(result.length == (byte1.length + byte2.length));
    }

    @Test
    public void byteMergerNull() {
        byte[] byte1 = null;
        byte[] byte2 = "12345".getBytes();
        byte[] result = ByteArrayUtils.byteMerger(null, byte2);
        Assert.assertTrue(result.length == byte2.length);

        byte[] result2 = ByteArrayUtils.byteMerger(byte2, null);
        Assert.assertTrue(result.length == byte2.length);

    }

    @Test
    public void subByte() {
        byte[] bytes = "123456".getBytes();
        byte[] result = ByteArrayUtils.subByte(bytes, 2, 4);
        Assert.assertTrue(result.length == 4);

        byte[] result2 = ByteArrayUtils.subByte(null, 2, 4);
        Assert.assertTrue(result2 == null);

    }
}