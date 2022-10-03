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

import com.baidu.cloud.starlight.api.common.Constants;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

/**
 * Created by liuruisen on 2020/3/19.
 */
public class NetUriUtilsTest {

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test
    public void parseQueryString() {
        Map<String, String> hashMap = NetUriUtils.parseQueryString("");
        Assert.assertTrue(hashMap.size() == 0);

        Map<String, String> hashMap2 = NetUriUtils.parseQueryString("name=123&key=456");
        Assert.assertTrue(hashMap2.get("name").equals("123"));

        Map<String, String> hashMap3 = NetUriUtils.parseQueryString("name=123&key=456&@@&key2=123");
        Assert.assertTrue(hashMap3.get("key2").equals("123"));

    }

    @Test
    public void toQueryString() {
        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        Assert.assertEquals(NetUriUtils.toQueryString(map), "key1=value1&key2=value2");

        Map<String, String> map2 = new HashMap<>();
        Assert.assertEquals(NetUriUtils.toQueryString(map2), "");

        Assert.assertEquals(NetUriUtils.toQueryString(null), "");
    }

    @Test
    public void getIpByHost() {

        System.out.println(NetUriUtils.getIpByHost("localhost"));
        Assert.assertEquals(NetUriUtils.getIpByHost("localhost"), "127.0.0.1");

        Assert.assertEquals(NetUriUtils.getIpByHost("fghjk"), "fghjk");
    }

    @Test
    public void getLocalHost() {
        Assert.assertEquals(NetUriUtils.getLocalHost(), NetUriUtils.getLocalHost());
    }

    @Test
    public void isValidAddress() {
        Assert.assertFalse(NetUriUtils.isValidAddress(null));
    }

    @Test
    public void getLocalHost2() {
        clearLocalHostCache();
        environmentVariables.set(Constants.EM_APP, "em-name");
        environmentVariables.set(Constants.EM_PLATFORM, "em-platform");
        environmentVariables.set(Constants.EM_PRODUCT_LINE, "em-product");

        environmentVariables.set(Constants.MATRIX_HOST_IP, "127.1.1.1");

        Assert.assertEquals("127.1.1.1", NetUriUtils.getLocalHost());

        clearLocalHostCache();
        environmentVariables.set(Constants.EM_IP, "127.1.1.2");
        Assert.assertEquals("127.1.1.2", NetUriUtils.getLocalHost());
    }

    @Test
    public void getLocalHost3() {
        environmentVariables.clear(Constants.EM_APP, Constants.MATRIX_HOST_IP);
        String localhostIp = NetUriUtils.getLocalHost();
        System.out.println(localhostIp);
    }

    private void clearLocalHostCache() {
        try {
            Class clazz = NetUriUtils.class;
            Field field = clazz.getDeclaredField("localHostIp");
            field.setAccessible(true);
            field.set(null, "");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}