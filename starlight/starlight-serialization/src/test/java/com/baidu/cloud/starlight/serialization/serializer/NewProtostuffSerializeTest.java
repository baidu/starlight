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
import com.baidu.cloud.starlight.serialization.serializer.model.Address;
import com.baidu.cloud.starlight.serialization.serializer.model.AllNull;
import com.baidu.cloud.starlight.serialization.serializer.model.ExtInfo;
import com.baidu.cloud.starlight.serialization.serializer.model.Sex;
import com.baidu.cloud.starlight.serialization.serializer.model.User;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtobufIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.DefaultIdStrategy;
import io.protostuff.runtime.IdStrategy;
import io.protostuff.runtime.RuntimeSchema;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;

/**
 * Created by liuruisen on 2021/1/19.
 */
public class NewProtostuffSerializeTest {

    protected DefaultIdStrategy newlyStrategy;

    protected DefaultIdStrategy newly3Strategy;

    protected DefaultIdStrategy defaultIdStrategy;

    protected DefaultIdStrategy nullStrategy;

    private User user;

    @Before
    public void before() {
        defaultIdStrategy = new DefaultIdStrategy(IdStrategy.DEFAULT_FLAGS);

        newly3Strategy =
            new DefaultIdStrategy(IdStrategy.DEFAULT_FLAGS | IdStrategy.COLLECTION_SCHEMA_ON_REPEATED_FIELDS
                | IdStrategy.MORPH_COLLECTION_INTERFACES | IdStrategy.MORPH_MAP_INTERFACES);

        newlyStrategy = new DefaultIdStrategy(
            IdStrategy.DEFAULT_FLAGS | IdStrategy.PRESERVE_NULL_ELEMENTS | IdStrategy.MORPH_COLLECTION_INTERFACES
                | IdStrategy.MORPH_MAP_INTERFACES | IdStrategy.MORPH_NON_FINAL_POJOS);

        nullStrategy = new DefaultIdStrategy(IdStrategy.DEFAULT_FLAGS | IdStrategy.PRESERVE_NULL_ELEMENTS);

        user = new User();
        user.setUserName("Test-user");
        user.setUserId(123L);
        user.setAge(18);
        user.setAlive(true);
        user.setBalance(11111.333d);
        user.setInfo("This is a human");
        user.setSalary(200.67f);
        user.setSex(Sex.MALE);

        Address address = new Address("Beijing");
        user.setAddress(address);
        List<String> tags = new LinkedList<>();
        tags.add("Key=Value");
        tags.add("Key2=Value2");
        user.setTags(tags);

        TreeSet<String> stringSet = new TreeSet<>();
        stringSet.add("val1");
        stringSet.add("val2");
        user.setSet(stringSet);

        ExtInfo extInfo1 = new ExtInfo("ext1", 123);
        ExtInfo extInfo2 = new ExtInfo("ext2", "test");
        LinkedList<ExtInfo> list = new LinkedList<>();
        list.add(extInfo1);
        list.add(extInfo2);
        user.setExtInfos(list);

        Object[] arrays = new Object[3];
        arrays[0] = 123;
        arrays[1] = address;
        arrays[2] = Sex.NONE;

        user.setArray(arrays);
    }

    @Test
    public void emptyList() {
        user.setTags(new LinkedList<>());

        byte[] bytes3 = serialize(user, User.class, defaultIdStrategy);
        User user3 = (User) deserialize(bytes3, User.class, defaultIdStrategy);
        System.out.println("defa ser -- defa deser " + user.equals(user3));
        System.out.println("");

        byte[] bytes4 = serialize(user, User.class, nullStrategy);
        User user4 = (User) deserialize(bytes4, User.class, nullStrategy);
        System.out.println("null ser -- null deser " + user.equals(user4));
        System.out.println("");

        byte[] bytes2 = serialize(user, User.class, newly3Strategy);
        User user2 = (User) deserialize(bytes2, User.class, newly3Strategy);
        System.out.println("newly3 ser -- newly3 deser " + user.equals(user2));
        System.out.println("");

        byte[] bytes1 = serialize(user, User.class, newlyStrategy);
        User user1 = (User) deserialize(bytes1, User.class, newlyStrategy);
        System.out.println("newly ser -- newly deser " + user.equals(user1));
        System.out.println("");

    }

    @Test
    public void listMidNull() {
        user.setTags(Arrays.asList("1", null, "3"));

        byte[] bytes2 = serialize(user, User.class, newly3Strategy);
        User user2 = (User) deserialize(bytes2, User.class, newly3Strategy);
        System.out.println("newly3 ser -- newly3 deser " + user.equals(user2));
        System.out.println("");

        byte[] bytes3 = serialize(user, User.class, defaultIdStrategy);
        User user3 = (User) deserialize(bytes3, User.class, defaultIdStrategy);
        System.out.println("defa ser -- defa deser " + user.equals(user3));
        System.out.println("");

        byte[] bytes4 = serialize(user, User.class, nullStrategy);
        User user4 = (User) deserialize(bytes4, User.class, nullStrategy);
        System.out.println("null ser -- null deser " + user.equals(user4));
        System.out.println("");

        byte[] bytes1 = serialize(user, User.class, newlyStrategy);
        User user1 = (User) deserialize(bytes1, User.class, newlyStrategy);
        System.out.println("newly ser -- newly deser " + user.equals(user1));
        System.out.println("");
    }

    @Test
    public void subList() {
        List<String> stringList = Arrays.asList("1", "2", "3", "4", "5");
        user.setTags(stringList.subList(0, 2));

        try {
            byte[] bytes3 = serialize(user, User.class, defaultIdStrategy);
            User user3 = (User) deserialize(bytes3, User.class, defaultIdStrategy);
            System.out.println("defal ser -- defal deser " + user.equals(user3));
        } catch (CodecException e) {
            e.printStackTrace();
        }

        try {
            byte[] bytes4 = serialize(user, User.class, nullStrategy);
            User user4 = (User) deserialize(bytes4, User.class, nullStrategy);
            System.out.println("null ser -- null deser " + user.equals(user4));
        } catch (CodecException e) {
            e.printStackTrace();
        }

        try {
            byte[] bytes2 = serialize(user, User.class, newly3Strategy);
            User user2 = (User) deserialize(bytes2, User.class, newly3Strategy);
            System.out.println("new3 ser -- new3 deser " + user.equals(user2));
        } catch (CodecException e) {
            e.printStackTrace();
        }

        try {
            byte[] bytes1 = serialize(user, User.class, newlyStrategy);
            User user1 = (User) deserialize(bytes1, User.class, newlyStrategy);
            System.out.println("new ser -- new deser " + user.equals(user1));
        } catch (CodecException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void allNull() {

        ExtInfo extInfo = new ExtInfo();
        byte[] bytes = serialize(extInfo, ExtInfo.class, nullStrategy);

        System.out.println(Arrays.toString(bytes));

        ExtInfo extInfo1 = (ExtInfo) deserialize(bytes, ExtInfo.class, nullStrategy);
        System.out.println(extInfo1);

        ExtInfo extInfo2 = new ExtInfo();
        byte[] bytes2 = serialize(extInfo2, ExtInfo.class, defaultIdStrategy);
        System.out.println(Arrays.toString(bytes2));

        ExtInfo extInfo3 = (ExtInfo) deserialize(bytes2, ExtInfo.class, defaultIdStrategy);
        System.out.println(extInfo3);

        AllNull allNull = new AllNull();
        byte[] bytes3 = serialize(allNull, AllNull.class, nullStrategy);

        System.out.println(Arrays.toString(bytes3));

        AllNull allNull2 = (AllNull) deserialize(bytes3, AllNull.class, nullStrategy);
        System.out.println(allNull2);

        AllNull allNull3 = new AllNull();
        byte[] bytes4 = serialize(allNull3, AllNull.class, defaultIdStrategy);
        System.out.println(Arrays.toString(bytes4));

        AllNull allNull5 = (AllNull) deserialize(bytes4, AllNull.class, defaultIdStrategy);
        System.out.println(allNull5);

        List<ExtInfo> extInfos = new ArrayList<>();
        extInfos.add(new ExtInfo("Key1", "Value1"));
        extInfos.add(new ExtInfo("Key2", "Value2"));

        byte[] listNullBytes = serialize(extInfos, extInfos.getClass(), newly3Strategy);
        System.out.println(Arrays.toString(listNullBytes));

        try {
            List<ExtInfo> extInfos2 = (List<ExtInfo>) deserialize(listNullBytes, List.class, newly3Strategy);
            System.out.println(extInfos2);
        } catch (CodecException e) {
            assertEquals(CodecException.DESERIALIZE_EXCEPTION, e.getCode());
        }
    }

    @Test
    public void keySet() {
        Map<String, String> map = new HashMap<>();
        map.put("1", "1");
        map.put("2", "2");
        map.put("3", "3");
        TreeSet sets = new TreeSet();
        sets.addAll(map.keySet());
        user.setSet(sets);

        try {
            byte[] bytes3 = serialize(user, User.class, defaultIdStrategy);
            User user3 = (User) deserialize(bytes3, User.class, defaultIdStrategy);
            System.out.println("defal ser -- defal deser " + user.equals(user3));
        } catch (CodecException e) {
            e.printStackTrace();
        }

        try {
            byte[] bytes4 = serialize(user, User.class, nullStrategy);
            User user4 = (User) deserialize(bytes4, User.class, nullStrategy);
            System.out.println("null ser -- null deser " + user.equals(user4));
        } catch (CodecException e) {
            e.printStackTrace();
        }

        try {
            byte[] bytes2 = serialize(user, User.class, newly3Strategy);
            User user2 = (User) deserialize(bytes2, User.class, newly3Strategy);
            System.out.println("new3 ser -- new3 deser " + user.equals(user2));
        } catch (CodecException e) {
            e.printStackTrace();
        }

        try {
            byte[] bytes1 = serialize(user, User.class, newlyStrategy);
            User user1 = (User) deserialize(bytes1, User.class, newlyStrategy);
            System.out.println("new ser -- new deser " + user.equals(user1));
        } catch (CodecException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void emptyMap() {

        user.setMap(Collections.emptyMap());

        try {
            byte[] bytes3 = serialize(user, User.class, defaultIdStrategy);
            User user3 = (User) deserialize(bytes3, User.class, defaultIdStrategy);
            System.out.println("defal ser -- defal deser " + user.equals(user3));
        } catch (CodecException e) {
            e.printStackTrace();
        }

        try {
            byte[] bytes4 = serialize(user, User.class, nullStrategy);
            User user4 = (User) deserialize(bytes4, User.class, nullStrategy);
            System.out.println("null ser -- null deser " + user.equals(user4));
        } catch (CodecException e) {
            e.printStackTrace();
        }

        try {
            byte[] bytes2 = serialize(user, User.class, newly3Strategy);
            User user2 = (User) deserialize(bytes2, User.class, newly3Strategy);
            System.out.println("new3 ser -- new3 deser " + user.equals(user2));
        } catch (CodecException e) {
            e.printStackTrace();
        }

        try {
            byte[] bytes1 = serialize(user, User.class, newlyStrategy);
            User user1 = (User) deserialize(bytes1, User.class, newlyStrategy);
            System.out.println("new ser -- new deser " + user.equals(user1));
        } catch (CodecException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test() {

        byte[] bytes = serialize(user, User.class, defaultIdStrategy);

        byte[] bytes2 = serialize(user, User.class, nullStrategy);

        try {
            User user1 = (User) deserialize(bytes, User.class, nullStrategy);
            System.out.println(user1);
        } catch (CodecException e) {
            e.printStackTrace();
        }

        try {
            User user2 = (User) deserialize(bytes2, User.class, defaultIdStrategy);
            System.out.println(user2);
        } catch (CodecException e) {
            e.printStackTrace();
        }
    }

    private static final int DEFAULT_ALLOCATE_NUM = 512;
    // Re-use (manage) this buffer to avoid allocating on every serialization
    private ThreadLocal<LinkedBuffer> buffer = new ThreadLocal<LinkedBuffer>() {
        @Override
        protected LinkedBuffer initialValue() {
            return LinkedBuffer.allocate(DEFAULT_ALLOCATE_NUM);
        }
    };

    protected Object deserialize(byte[] bytes, Class<?> type, IdStrategy idStrategy) throws CodecException {

        // use predefine object container as default
        try {
            Schema schema = RuntimeSchema.getSchema(type, idStrategy); // schema will be cached
            Object content = schema.newMessage();
            ProtobufIOUtil.mergeFrom(bytes, content, schema);
            return content;
        } catch (Exception e) {
            throw new CodecException(CodecException.DESERIALIZE_EXCEPTION,
                "Protostuff Deserialize error: " + e.getMessage(), e);
        }
    }

    protected byte[] serialize(Object obj, Class<?> type, IdStrategy idStrategy) throws CodecException {

        // use predefine object container as default
        try {
            Schema schema = RuntimeSchema.getSchema(type, idStrategy); // schema will be cached
            return ProtobufIOUtil.toByteArray(obj, schema, buffer.get());
        } catch (Exception e) {
            throw new CodecException(CodecException.SERIALIZE_EXCEPTION,
                "Protostuff serialize error: " + e.getMessage(), e);
        } finally {
            buffer.get().clear();
        }
    }
}
