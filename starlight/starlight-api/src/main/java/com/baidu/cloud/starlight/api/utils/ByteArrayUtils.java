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

/**
 * Created by liuruisen on 2020/2/20.
 */
public class ByteArrayUtils {

    /**
     * 合并byte[]数组 （不改变原数组）
     * 
     * @param bytes1
     * @param bytes2
     * @return 合并后的数组
     */
    public static byte[] byteMerger(byte[] bytes1, byte[] bytes2) {
        if (bytes1 == null || bytes1.length == 0) {
            return bytes2;
        }
        if (bytes2 == null || bytes2.length == 0) {
            return bytes1;
        }
        byte[] bytes3 = new byte[bytes1.length + bytes2.length];
        System.arraycopy(bytes1, 0, bytes3, 0, bytes1.length);
        System.arraycopy(bytes2, 0, bytes3, bytes1.length, bytes2.length);
        return bytes3;
    }

    /**
     * 截取byte数组 不改变原数组
     *
     * @param b 原数组
     * @param off 偏差值（索引）
     * @param length 长度
     * @return 截取后的数组
     */
    public static byte[] subByte(byte[] b, int off, int length) {
        if (b == null || b.length == 0) {
            return b;
        }
        byte[] b1 = new byte[length];
        System.arraycopy(b, off, b1, 0, length);
        return b1;
    }
}
