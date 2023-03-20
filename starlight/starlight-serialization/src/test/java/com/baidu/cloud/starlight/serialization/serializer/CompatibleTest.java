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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * 测试不同参数配置下的兼容性 1. 0 参数配置，默认场景 2. 3 参数配置，stargate场景 3. 5 参数配置， Created by liuruisen on 2021/1/19.
 */
public class CompatibleTest {

    private IdStrategy defaultStrategy;

    private IdStrategy newly3Strategy;

    private IdStrategy newlyStrategy;

    private IdStrategy nullStrategy;

    private User user;

    @Before
    public void before() {

        defaultStrategy = new DefaultIdStrategy(IdStrategy.DEFAULT_FLAGS);

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

        user.setTags(Arrays.asList("Key=Value", "Key2=Value2"));

        TreeSet<String> stringSet = new TreeSet<>();
        stringSet.add("val1");
        stringSet.add("val2");
        user.setSet(stringSet);

        ExtInfo extInfo1 = new ExtInfo("ext1", 123);
        ExtInfo extInfo2 = new ExtInfo("ext2", "test");
        LinkedList<ExtInfo> extInfos = new LinkedList<>();
        extInfos.add(extInfo1);
        extInfos.add(extInfo2);
        user.setExtInfos(extInfos);

        Object[] arrays = new Object[3];
        arrays[0] = 123;
        arrays[1] = address;
        arrays[2] = Sex.NONE;

        user.setArray(arrays);

    }

    @Test
    public void serializeCompatibleFullMsg() {
        System.out.println("--- Full msg compatible ---");
        serializeCompatible(user);
    }

    @Test
    public void serializeCompatibleEmptyList() {
        System.out.println("--- Empty list compatible ---");
        user.setTags(Collections.emptyList());
        serializeCompatible(user);

        user.setTags(new ArrayList<>());
        serializeCompatible(user);
    }

    @Test
    public void serializeCompatibleEmptySet() {
        System.out.println("--- Empty set compatible ---");
        user.setSet(new TreeSet<>());
        serializeCompatible(user);
    }

    @Test
    public void serializeCompatibleEmptyArray() {
        System.out.println("--- Empty array compatible ---");
        user.setArray(new Object[] {});
        serializeCompatible(user);
    }

    @Test
    public void serializeCompatibleListMiddNull() {
        System.out.println("--- List middle null compatible ---");
        user.setTags(Arrays.asList("1", null, "3"));
        serializeCompatible(user);
    }

    @Test
    public void serializeCompatibleSetMiddNull() {
        System.out.println("--- Set middle null compatible ---");
        try {
            TreeSet<String> set = new TreeSet<>();
            set.add("1");
            set.add(null);
            set.add("3");
            user.setSet(set);
            serializeCompatible(user);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void serializeCompatibleArrayMiddNull() {
        System.out.println("--- Array middle null compatible ---");
        Object[] arrsy = new Object[3];
        arrsy[0] = 1;
        arrsy[1] = null;
        arrsy[2] = "hello";
        user.setArray(arrsy);
        serializeCompatible(user);
    }

    @Test
    public void serializeCompatibleSubList() {
        System.out.println("--- Sub list compatible ---");
        List<String> stringList = Arrays.asList("1", "2", "3", "4", "5");
        user.setTags(stringList.subList(0, 2));
        serializeCompatible(user);
    }

    @Test
    public void serializeCompatibleMapKeySet() {
        System.out.println("--- Map keyset compatible ---");
        Map<String, String> map = new HashMap<>();
        map.put("1", "1");
        map.put("2", "2");
        map.put("3", "3");
        TreeSet treeSet = new TreeSet();
        treeSet.addAll(map.keySet());
        user.setSet(treeSet);
        serializeCompatible(user);
    }

    @Test
    public void serializeCompatibleMiddleNull() {
        System.out.println("--- Middle null compatible ---");
        user.setAddress(null);
        serializeCompatible(user);
    }

    @Test
    public void serializeSubListWithNUllStrategy() {
        System.out.println("--- Sub List with null strategy compatible ---");
        List<String> stringList = Arrays.asList("1", "2", "3", "4", "5");
        user.setTags(stringList.subList(0, 2));

        byte[] nullSerBytes = serialize(user, User.class, nullStrategy);
        User result = (User) deserialize(nullSerBytes, User.class, nullStrategy);
        System.out.println("null ser -  null deser : " + user.equals(result));
    }

    @Test
    public void serializeKeySetWithNUllStrategy() {
        System.out.println("--- key set with null strategy compatible ---");
        Map<String, String> map = new HashMap<>();
        map.put("1", "1");
        map.put("2", "2");
        map.put("3", "3");
        TreeSet treeSet = new TreeSet();
        treeSet.addAll(map.keySet());
        user.setSet(treeSet);

        byte[] nullSerBytes = serialize(user, User.class, nullStrategy);
        User result = (User) deserialize(nullSerBytes, User.class, nullStrategy);
        System.out.println("null ser -  null deser : " + user.equals(result));
    }

    protected void serializeCompatible(User user) {
        // serialize
        byte[] defaultSerBytes = serialize(user, User.class, defaultStrategy);
        assertNotNull(defaultSerBytes);
        assertTrue(defaultSerBytes.length > 0);
        System.out.println("Default bytes " + Arrays.toString(defaultSerBytes));
        System.out.println("");

        byte[] newly3SerBytes = serialize(user, User.class, newly3Strategy);
        assertNotNull(newly3SerBytes);
        assertTrue(newly3SerBytes.length > 0);
        System.out.println("Newly3 bytes " + Arrays.toString(newly3SerBytes));
        System.out.println("");

        byte[] newlySerBytes = serialize(user, User.class, newlyStrategy);
        assertNotNull(newlySerBytes);
        assertTrue(newlySerBytes.length > 0);
        System.out.println("Newly bytes " + Arrays.toString(newlySerBytes));
        System.out.println("");

        byte[] nullBytes = serialize(user, User.class, nullStrategy);
        assertNotNull(nullBytes);
        assertTrue(nullBytes.length > 0);
        System.out.println("Null bytes " + Arrays.toString(nullBytes));
        System.out.println("");

        // deserialize
        try {
            User defaultUser = (User) deserialize(defaultSerBytes, User.class, defaultStrategy);
            assertNotNull(defaultUser);
            System.out.println("default ser - default deser : " + user.equals(defaultUser));
        } catch (CodecException e) {
            System.out.println("Use default strategy to deserialize default bytes failed, causeb by " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("");

        try {
            User threeUser = (User) deserialize(newly3SerBytes, User.class, newly3Strategy);
            assertNotNull(threeUser);
            System.out.println("newly3 ser - newly3 deser : " + user.equals(threeUser));
        } catch (CodecException e) {
            System.out.println("Use newly3 strategy to deserialize newly3 bytes failed, causeb by " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("");

        try {
            User fiveUser = (User) deserialize(newlySerBytes, User.class, newlyStrategy);
            assertNotNull(fiveUser);
            System.out.println("newly ser - newly deser : " + user.equals(fiveUser));
        } catch (CodecException e) {
            System.out.println("Use newly strategy to deserialize newly bytes failed, causeb by " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("");

        try {
            User nullUser = (User) deserialize(nullBytes, User.class, nullStrategy);
            assertNotNull(nullUser);
            System.out.println("null ser - null deser : " + user.equals(nullUser));
        } catch (CodecException e) {
            System.out.println("Use null strategy to deserialize null bytes failed, causeb by " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("");

        // deserialize compatible
        try {
            User useNewly3StrategyToDesDefBytes = (User) deserialize(defaultSerBytes, User.class, newly3Strategy);
            assertNotNull(useNewly3StrategyToDesDefBytes);
            System.out.println("default ser - newly3 deser : " + user.equals(useNewly3StrategyToDesDefBytes));
        } catch (CodecException e) {
            System.out.println("Use newly3 strategy to deserialize default bytes failed, causeb by " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("");

        try {
            User useNewlyStrategyToDesDefBytes = (User) deserialize(defaultSerBytes, User.class, newlyStrategy);
            assertNotNull(useNewlyStrategyToDesDefBytes);
            System.out.println("default ser - newly deser : " + user.equals(useNewlyStrategyToDesDefBytes));
        } catch (CodecException e) {
            System.out.println("Use newly strategy to deserialize default bytes failed, causeb by " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("");

        try {
            User useNullStrategyToDesDefBytes = (User) deserialize(defaultSerBytes, User.class, nullStrategy);
            assertNotNull(useNullStrategyToDesDefBytes);
            System.out.println("default ser - null deser : " + user.equals(useNullStrategyToDesDefBytes));
        } catch (CodecException e) {
            System.out.println("Use null strategy to deserialize default bytes failed, causeb by " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("");

        try {
            User useNewly3StraToDesNewBytes = (User) deserialize(newlySerBytes, User.class, newly3Strategy);
            assertNotNull(useNewly3StraToDesNewBytes);
            System.out.println("newly ser - newly3 deser : " + user.equals(useNewly3StraToDesNewBytes));
        } catch (CodecException e) {
            System.out.println("Use newly3 strategy to deserialize newly bytes failed, causeb by " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("");

        try {
            User useDefStraToDesNewBytes = (User) deserialize(newlySerBytes, User.class, defaultStrategy);
            assertNotNull(useDefStraToDesNewBytes);
            System.out.println("newly ser - default deser : " + user.equals(useDefStraToDesNewBytes));
        } catch (CodecException e) {
            System.out.println("Use default strategy to deserialize newly bytes failed, causeb by " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("");

        try {
            User useNullStraToDesNewBytes = (User) deserialize(newlySerBytes, User.class, nullStrategy);
            assertNotNull(useNullStraToDesNewBytes);
            System.out.println("newly ser - null deser : " + user.equals(useNullStraToDesNewBytes));
        } catch (CodecException e) {
            System.out.println("Use null strategy to deserialize newly bytes failed, causeb by " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("");

        try {
            User useDefStraToDesNewly3Bytes = (User) deserialize(newly3SerBytes, User.class, defaultStrategy);
            assertNotNull(useDefStraToDesNewly3Bytes);
            System.out.println("newly3 ser - default deser : " + user.equals(useDefStraToDesNewly3Bytes));
        } catch (CodecException e) {
            System.out.println("Use default strategy to deserialize newly3 bytes failed, causeb by " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("");

        try {
            User useNewStraToDesNewly3Bytes = (User) deserialize(newly3SerBytes, User.class, newlyStrategy);
            assertNotNull(useNewStraToDesNewly3Bytes);
            System.out.println("newly3 ser - newly deser : " + user.equals(useNewStraToDesNewly3Bytes));
        } catch (CodecException e) {
            System.out.println("Use newly strategy to deserialize newly3 bytes failed, causeb by " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("");

        try {
            User useNullStraToDesNewly3Bytes = (User) deserialize(newly3SerBytes, User.class, nullStrategy);
            assertNotNull(useNullStraToDesNewly3Bytes);
            System.out.println("newly3 ser - null deser : " + user.equals(useNullStraToDesNewly3Bytes));
        } catch (CodecException e) {
            System.out.println("Use null strategy to deserialize newly3 bytes failed, causeb by " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("");

        try {
            User useDefStraToDesNullBytes = (User) deserialize(nullBytes, User.class, defaultStrategy);
            assertNotNull(useDefStraToDesNullBytes);
            System.out.println("null ser - default deser : " + user.equals(useDefStraToDesNullBytes));
        } catch (CodecException e) {
            System.out.println("Use default strategy to deserialize null bytes failed, causeb by " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("");

        try {
            User useNew3StraToDesNullBytes = (User) deserialize(nullBytes, User.class, newly3Strategy);
            assertNotNull(useNew3StraToDesNullBytes);
            System.out.println("null ser - new3 deser : " + user.equals(useNew3StraToDesNullBytes));
        } catch (CodecException e) {
            System.out.println("Use new3 strategy to deserialize null bytes failed, causeb by " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("");

        try {
            User useNewStraToDesNullBytes = (User) deserialize(nullBytes, User.class, newlyStrategy);
            assertNotNull(useNewStraToDesNullBytes);
            System.out.println("null ser - newly deser : " + user.equals(useNewStraToDesNullBytes));
        } catch (CodecException e) {
            System.out.println("Use newly strategy to deserialize null bytes failed, causeb by " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("");

    }

    private static final int DEFAULT_ALLOCATE_NUM = 512;
    // Re-use (manage) this buffer to avoid allocating on every serialization
    private ThreadLocal<LinkedBuffer> buffer = new ThreadLocal<LinkedBuffer>() {
        @Override
        protected LinkedBuffer initialValue() {
            return LinkedBuffer.allocate(DEFAULT_ALLOCATE_NUM);
        }
    };

    private Object deserialize(byte[] bytes, Class<?> type, IdStrategy idStrategy) throws CodecException {

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

    private byte[] serialize(Object obj, Class<?> type, IdStrategy idStrategy) throws CodecException {

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
