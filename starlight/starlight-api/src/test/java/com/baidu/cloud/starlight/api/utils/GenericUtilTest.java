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
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by liuruisen on 2020/4/29.
 */
public class GenericUtilTest {

    @Test
    public void isGenericCall() {
        Request request = new RpcRequest();
        request.setMethodName("$invoke");
        Assert.assertTrue(GenericUtil.isGenericCall(request));
        Assert.assertFalse(GenericUtil.isGenericCall(new RpcRequest()));
    }

    @Test
    public void markGeneric() {
        Request request = new RpcRequest();
        GenericUtil.markGeneric(request);
        Assert.assertTrue(request.getAttachmentKv().size() > 0);
        Assert.assertTrue((Boolean) request.getAttachmentKv().get(Constants.IS_GENERIC_KEY));
    }

    @Test
    public void isGenericMsg() {
        Request request = new RpcRequest();
        Assert.assertFalse(GenericUtil.isGenericMsg(request));
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.IS_GENERIC_KEY, Boolean.TRUE);
        request.setAttachmentKv(map);
        Assert.assertTrue(GenericUtil.isGenericMsg(request));
    }
}