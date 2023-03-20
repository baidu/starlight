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

import io.protostuff.runtime.DefaultIdStrategy;
import io.protostuff.runtime.IdStrategy;
import com.baidu.cloud.starlight.api.exception.CodecException;
import com.baidu.cloud.starlight.api.model.Wrapper;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by liuruisen on 2020/3/19.
 */
public class ProtoStuffSerializerTest {

    private ProtoStuffSerializer serializer = new ProtoStuffSerializer();

    private static final int idStrategyFlag = 0 | IdStrategy.AUTO_LOAD_POLYMORPHIC_CLASSES;

    @Test
    public void serialize() {
        byte[] bytes1 = serializer.serialize("Test", String.class);
        String str = (String) serializer.deserialize(bytes1, String.class);
        Assert.assertTrue(str.equals("Test"));
    }

    @Test
    public void serializeIdStrategy() {
        byte[] bytes2 = serializer.serialize("Test", String.class, idStrategyFlag);
        String str2 = (String) serializer.deserialize(bytes2, String.class);
        Assert.assertTrue(str2.equals("Test"));
    }

    @Test
    public void serializeException() {
        try {
            // message null
            serializer.serialize(null, String.class);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode().equals(CodecException.SERIALIZE_EXCEPTION));
        }

        try {
            // type null
            serializer.serialize("123", null);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode().equals(CodecException.SERIALIZE_EXCEPTION));
        }

        try {
            // not support single long int boolean
            serializer.serialize(1l, long.class);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode().equals(CodecException.SERIALIZE_EXCEPTION));
        }

        try {
            // serialize exception
            serializer.serialize(1, String.class);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode().equals(CodecException.SERIALIZE_EXCEPTION));
        }

    }

    @Test
    public void serializeIdStrategyException() {
        try {
            // message null
            serializer.serialize(null, String.class, idStrategyFlag);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode().equals(CodecException.SERIALIZE_EXCEPTION));
        }

        try {
            // type null
            serializer.serialize("123", null, idStrategyFlag);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode().equals(CodecException.SERIALIZE_EXCEPTION));
        }

        try {
            // not support single long int boolean
            serializer.serialize(1L, long.class, idStrategyFlag);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode().equals(CodecException.SERIALIZE_EXCEPTION));
        }

        try {
            // serialize exception
            serializer.serialize(1, String.class, idStrategyFlag);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode().equals(CodecException.SERIALIZE_EXCEPTION));
        }
    }

    @Test
    public void deserializeException() {

        try {
            // bytes null
            serializer.deserialize(null, String.class);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode().equals(CodecException.DESERIALIZE_EXCEPTION));
        }

        try {
            // bytes length 0
            serializer.deserialize(new byte[0], String.class);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode().equals(CodecException.DESERIALIZE_EXCEPTION));
        }

        byte[] bytes1 = serializer.serialize(1, Integer.class);
        try {
            // type null
            serializer.deserialize(bytes1, null);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode().equals(CodecException.DESERIALIZE_EXCEPTION));
        }

        try {
            // type isPrimitive
            serializer.deserialize(bytes1, int.class);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode().equals(CodecException.DESERIALIZE_EXCEPTION));
        }

        try {
            // deserialize exception
            serializer.deserialize(bytes1, String.class);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode().equals(CodecException.DESERIALIZE_EXCEPTION));
        }
    }

    @Test
    public void deserializeIdStrategyException() {
        IdStrategy strategy = new DefaultIdStrategy();

        try {
            // bytes null
            serializer.deserialize(null, String.class, idStrategyFlag);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode().equals(CodecException.DESERIALIZE_EXCEPTION));
        }

        try {
            // bytes length 0
            serializer.deserialize(new byte[0], String.class, idStrategyFlag);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode().equals(CodecException.DESERIALIZE_EXCEPTION));
        }

        byte[] bytes1 = serializer.serialize(1, Integer.class, idStrategyFlag);
        try {
            // type null
            serializer.deserialize(bytes1, null);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode().equals(CodecException.DESERIALIZE_EXCEPTION));
        }

        try {
            // type isPrimitive
            serializer.deserialize(bytes1, int.class, idStrategyFlag);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode().equals(CodecException.DESERIALIZE_EXCEPTION));
        }

        try {
            // deserialize exception
            serializer.deserialize(bytes1, String.class, idStrategyFlag);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode().equals(CodecException.DESERIALIZE_EXCEPTION));
        }
    }

    @Test
    public void wrapperSerialize() {
        Map<String, String> map = new HashMap<>();
        map.put("Key", "Value");
        Object[] args = new Object[] {"MethodName", new Object[] {"hello", map}};
        Wrapper wrapper = new Wrapper(args);

        byte[] bytes = serializer.serialize(wrapper, Wrapper.class);

        Wrapper params = (Wrapper) serializer.deserialize(bytes, Wrapper.class);

        Object[] objects = (Object[]) params.getObj();
        Assert.assertEquals(objects[0], "MethodName");
        Object[] objects1 = (Object[]) objects[1];
        Assert.assertEquals(objects1[0], "hello");
        Assert.assertEquals(((Map) objects1[1]).get("Key"), "Value");
    }

    @Test
    public void exceptionConvert() {

        ProtoStuffSerializer serializer = new ProtoStuffSerializer();

        AsListModel model = new AsListModel();
        List<String> strList = new ArrayList<>();
        strList.add("45600");
        model.setAsList(strList);

        byte[] bytes = serializer.serialize(model, AsListModel.class);

        try {
            serializer.deserialize(bytes, AsListModel.class, 4);
        } catch (CodecException e) {
            Assert.assertEquals(CodecException.DESERIALIZE_EXCEPTION, e.getCode());
            System.out.println(e);
        }

        WrapModel wrapModel = new WrapModel();
        wrapModel.setAsListModel(model);

        byte[] bytes2 = serializer.serialize(wrapModel, WrapModel.class, 6);

        try {
            serializer.deserialize(bytes2, WrapModel.class, 6);
        } catch (CodecException e) {
            Assert.assertEquals(CodecException.DESERIALIZE_EXCEPTION, e.getCode());
            System.out.println(e);
        }

    }

    private class WrapModel {
        private AsListModel asListModel;

        public AsListModel getAsListModel() {
            return asListModel;
        }

        public void setAsListModel(AsListModel asListModel) {
            this.asListModel = asListModel;
        }
    }

    private class AsListModel {
        List<String> asList = Arrays.asList("123");

        public List<String> getAsList() {
            return asList;
        }

        public void setAsList(List<String> asList) {
            this.asList = asList;
        }
    }
}