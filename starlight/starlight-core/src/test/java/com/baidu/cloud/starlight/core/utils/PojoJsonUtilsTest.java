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
 
package com.baidu.cloud.starlight.core.utils;

import com.baidu.cloud.starlight.core.integrate.model.ExtInfo;
import com.baidu.cloud.starlight.core.integrate.model.User;
import com.baidu.cloud.thirdparty.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by liuruisen on 2021/3/16.
 */
public class PojoJsonUtilsTest {

    @Test
    public void realize() throws Exception {

        String pattern = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        String date = simpleDateFormat.format(new Date());

        Map<String, Object> genericTest = new HashMap<>();
        genericTest.put("date", date);
        Object[] objects = PojoJsonUtils.realize(new Object[] {genericTest}, new Type[] {GenericTest.class});

        assertNotNull(objects);
        assertEquals(date, simpleDateFormat.format(((GenericTest) objects[0]).getDate()));

        long dateTime = new Date().getTime();
        Map<String, Object> genericTest2 = new HashMap<>();
        genericTest2.put("date", dateTime);

        Object[] objects2 = PojoJsonUtils.realize(new Object[] {genericTest2}, new Type[] {GenericTest.class});
        assertNotNull(objects2);
        assertEquals(dateTime, ((GenericTest) objects2[0]).getDate().getTime());
    }

    @Test
    public void realizeGeneric() {

        Map<String, Object> extInf = new HashMap<>();
        extInf.put("key", "asdhjashdkjas");
        extInf.put("value", "123123");

        Map<String, Object> extInf2 = new HashMap<>();
        extInf2.put("key", "123123123");
        extInf2.put("value", 45678);

        List<Map<String, Object>> extInfos = new ArrayList<>();
        extInfos.add(extInf);
        extInfos.add(extInf2);

        try {
            Method method = this.getClass().getMethod("genericMethod", Long.class, String.class, List.class);
            Type[] paramTypes = method.getGenericParameterTypes();

            String name = new ObjectMapper().writeValueAsString(new ExtInfo("key", "value"));
            Object[] args = PojoJsonUtils.realize(new Object[] {1L, name, extInfos}, paramTypes);

            for (Object arg : args) {
                System.out.println(arg);
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public User genericMethod(Long id, String name, List<ExtInfo> extInfos) {
        return new User();
    }

    @Test
    public void generalize() throws Exception {
        GenericTest genericTest = new GenericTest();
        genericTest.setDate(new Date());

        Object result = PojoJsonUtils.generalize(genericTest);
        assertTrue(result instanceof Map);
    }

    public static class GenericTest {
        private Date date;

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }
    }
}