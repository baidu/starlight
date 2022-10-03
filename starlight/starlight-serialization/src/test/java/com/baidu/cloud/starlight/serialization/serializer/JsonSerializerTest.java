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
import com.baidu.cloud.starlight.serialization.serializer.model.User;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by liuruisen on 2020/5/27.
 */
public class JsonSerializerTest {

    private JsonSerializer jsonSerializer = new JsonSerializer();

    @Test
    public void serialize() {
        User user = new User();
        user.setTags(Arrays.asList("123", "1111"));
        user.setUserId(12);
        user.setUserName("userName");

        byte[] bytes = jsonSerializer.serialize(user, User.class);
        Assert.assertTrue(bytes.length > 0);

    }

    @Test
    public void serializeException() {
        User user = null;
        // object is null
        try {
            jsonSerializer.serialize(user, User.class);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode().equals(CodecException.SERIALIZE_EXCEPTION));
            Assert.assertTrue(e.getMessage().contains("json-serialize is null"));
        }

        // object type is not equals
        user = new User();
        try {
            jsonSerializer.serialize(user, String.class);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode().equals(CodecException.SERIALIZE_EXCEPTION));
            Assert.assertTrue(e.getMessage().contains("json-serialize is illegal"));
        }

        // not support type
        try {
            jsonSerializer.serialize(new Integer(1), Integer.class);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode().equals(CodecException.SERIALIZE_EXCEPTION));
            Assert.assertTrue(e.getMessage().contains("json-serialize is illegal"));
        }
    }

    @Test
    public void deserialize() {
        User user = new User();
        user.setTags(Arrays.asList("123", "1111"));
        user.setUserId(12);
        user.setUserName("userName");
        byte[] bytes = jsonSerializer.serialize(user, User.class);
        User user2 = (User) jsonSerializer.deserialize(bytes, User.class);

        Assert.assertTrue(user2.getUserName().equals(user.getUserName()));
        Assert.assertTrue(user2.getUserId() == user.getUserId());
        Assert.assertTrue(user2.getTags().equals(user.getTags()));

        // serialize and deserialize String
        byte[] stringBytes = jsonSerializer.serialize("Test", String.class);
        String result = (String) jsonSerializer.deserialize(stringBytes, String.class);
        Assert.assertTrue(result.equals("Test"));
    }

    @Test
    public void deserializeException() {

        try {
            jsonSerializer.deserialize(new byte[0], User.class);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode().equals(CodecException.DESERIALIZE_EXCEPTION));
            Assert.assertTrue(e.getMessage().contains("empty"));
        }

        try {
            jsonSerializer.deserialize("123".getBytes(), Integer.class);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode().equals(CodecException.DESERIALIZE_EXCEPTION));
            Assert.assertTrue(e.getMessage().contains("illegal"));
        }

    }

    @Test
    public void isSupportTest() {
        // map collection
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        try {
            jsonSerializer.serialize(map, map.getClass());
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode().equals(CodecException.SERIALIZE_EXCEPTION));
            Assert.assertTrue(e.getMessage().contains("illegal"));
        }

        List<String> list = new ArrayList<>();
        list.add("123");
        try {
            jsonSerializer.serialize(list, list.getClass());
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode().equals(CodecException.SERIALIZE_EXCEPTION));
            Assert.assertTrue(e.getMessage().contains("illegal"));
        }

        // arr
        String[] strings = new String[] {"123", "123"};
        try {
            jsonSerializer.serialize(strings, strings.getClass());
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode().equals(CodecException.SERIALIZE_EXCEPTION));
            Assert.assertTrue(e.getMessage().contains("illegal"));
        }

        // primitive
        try {
            jsonSerializer.serialize(1, int.class);
        } catch (CodecException e) {
            Assert.assertTrue(e.getCode().equals(CodecException.SERIALIZE_EXCEPTION));
            Assert.assertTrue(e.getMessage().contains("illegal"));
        }

    }

    @Test
    public void deserilizeByType() throws NoSuchMethodException {
        User user = new User();
        user.setUserId(123L);

        List<User> users = new ArrayList<>();
        users.add(user);

        byte[] bytes = jsonSerializer.serialize(users,
            this.getClass().getMethod("listUsers", List.class).getGenericParameterTypes()[0]);

        List<User> listResult = (List<User>) jsonSerializer.deserialize(bytes,
            this.getClass().getMethod("listUsers", List.class).getGenericParameterTypes()[0]);

        assertNotNull(listResult);
        assertEquals(1, listResult.size());

        Map<String, User> userMap = new HashMap<>();
        userMap.put("test-1", user);

        byte[] bytes2 = jsonSerializer.serialize(userMap,
            this.getClass().getMethod("mapUsers", Map.class).getGenericParameterTypes()[0]);

        Map<String, User> mapResult = (Map<String, User>) jsonSerializer.deserialize(bytes2,
            this.getClass().getMethod("mapUsers", Map.class).getGenericParameterTypes()[0]);

        assertNotNull(mapResult);
        assertEquals(1, mapResult.size());

        TestObj<User> testObj = new TestObj<>();
        testObj.setData(user);

        byte[] bytes3 = jsonSerializer.serialize(testObj,
            this.getClass().getMethod("userTestObj", TestObj.class).getGenericParameterTypes()[0]);

        TestObj<User> objResult = (TestObj<User>) jsonSerializer.deserialize(bytes3,
            this.getClass().getMethod("userTestObj", TestObj.class).getGenericParameterTypes()[0]);
        assertNotNull(objResult);
        assertEquals(123L, objResult.data.getUserId());
    }

    public List<User> listUsers(List<User> users) {
        return null;
    }

    public Map<String, User> mapUsers(Map<String, User> users) {
        return null;
    }

    public TestObj<User> userTestObj(TestObj<User> users) {
        return null;
    }

    public static class TestObj<T> {
        T data;

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }
    }

}