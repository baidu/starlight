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
 
package com.baidu.cloud.starlight.serialization.serializer;

import com.baidu.cloud.starlight.api.exception.CodecException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by liuruisen on 2020/9/1.
 */
public class DyuProtostuffSerializerTest {

    private DyuProtostuffSerializer serializer = new DyuProtostuffSerializer();

    @Test
    public void testSerialize() {
        byte[] bytes1 = serializer.serialize("Test", String.class);
        String str = (String) serializer.deserialize(bytes1, String.class);
        Assert.assertTrue(str.equals("Test"));

    }

    @Test
    public void testSerializeError() {
        try {
            // message null
            serializer.serialize(null, String.class);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode() == CodecException.SERIALIZE_EXCEPTION);
        }

        try {
            // type null
            serializer.serialize("123", null);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode() == CodecException.SERIALIZE_EXCEPTION);
        }

        try {
            // not support single long int boolean
            serializer.serialize(1l, long.class);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode() == CodecException.SERIALIZE_EXCEPTION);
        }

        try {
            // serialize exception
            serializer.serialize(1, String.class);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode() == CodecException.SERIALIZE_EXCEPTION);
        }
    }

    @Test
    public void testDeserializeError() {
        try {
            // bytes null
            serializer.deserialize(null, String.class);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode() == CodecException.DESERIALIZE_EXCEPTION);
        }

        try {
            // bytes length 0
            serializer.deserialize(new byte[0], String.class);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode() == CodecException.DESERIALIZE_EXCEPTION);
        }

        byte[] bytes1 = serializer.serialize(1, Integer.class);
        try {
            // type null
            serializer.deserialize(bytes1, null);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode() == CodecException.DESERIALIZE_EXCEPTION);
        }

        try {
            // type isPrimitive
            serializer.deserialize(bytes1, int.class);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode() == CodecException.DESERIALIZE_EXCEPTION);
        }

        try {
            // deserialize exception
            serializer.deserialize(bytes1, String.class);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode() == CodecException.DESERIALIZE_EXCEPTION);
        }
    }
}