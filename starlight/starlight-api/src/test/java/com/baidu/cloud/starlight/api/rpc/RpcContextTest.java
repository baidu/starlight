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
 
package com.baidu.cloud.starlight.api.rpc;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by liuruisen on 2020/9/2.
 */
public class RpcContextTest {

    @Test
    public void testContext() {
        Assert.assertNotNull(RpcContext.getContext());
        Assert.assertNotNull(RpcContext.getContext().get());
        Assert.assertEquals(0, RpcContext.getContext().get().size());
        RpcContext.getContext().set("Key", "Value");
        Assert.assertEquals(1, RpcContext.getContext().get().size());
        Assert.assertEquals("Value", RpcContext.getContext().get().get("Key"));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("Key1", "Value1");
        map.put("Key2", "Value2");
        RpcContext.getContext().set(map);
        Assert.assertEquals(2, RpcContext.getContext().get().size());
        Assert.assertEquals("Value2", RpcContext.getContext().get().get("Key2"));
        RpcContext.getContext().set(new HashMap<>());
        Assert.assertEquals(0, RpcContext.getContext().get().size());
        RpcContext.removeContext();
    }

    @Test
    public void testGetSet() {
        RpcContext.getContext().setRequestID("requestId");
        RpcContext.getContext().setSessionID("sessionId");
        RpcContext.getContext().setRemoteAddress("127.0.0.1", 9999);

        assertEquals("requestId", RpcContext.getContext().getRequestID());
        assertEquals("sessionId", RpcContext.getContext().getSessionID());
        assertEquals("127.0.0.1:9999", RpcContext.getContext().getRemoteAddressString());
    }

}