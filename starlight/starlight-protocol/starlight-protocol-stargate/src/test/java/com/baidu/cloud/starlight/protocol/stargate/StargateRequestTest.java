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
 
package com.baidu.cloud.starlight.protocol.stargate;

import com.baidu.cloud.starlight.api.common.URI;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

/**
 * Created by liuruisen on 2020/7/27.
 */
public class StargateRequestTest {

    @Test
    public void requestTest() {
        StargateRequest stargateRequest = new StargateRequest("123");
        URI uri = new URI.Builder("stargate", "127.0.0.1", 8888).build();
        stargateRequest.setUri(uri);
        stargateRequest.setMethodName("method");
        stargateRequest.setParameters(new Object[] {"object"});
        stargateRequest.setParameterTypes(new Class[] {String.class});
        stargateRequest.setSerial("1");

        Assert.assertEquals(stargateRequest.getUri(), uri);
        Assert.assertEquals(stargateRequest.getId(), "1");
        Assert.assertEquals(stargateRequest.getMethodName(), "method");
        Assert.assertEquals(stargateRequest.getParameters().length, 1);
        Assert.assertEquals(stargateRequest.getParameterTypes().length, 1);

        Assert.assertNull(stargateRequest.getAttachment("Key2"));
        stargateRequest.setAttachment("Key2", "Value2");
        Assert.assertEquals(stargateRequest.getAttachment("Key2"), "Value2");

        stargateRequest.setAttachments(new HashMap<String, Object>());
        Assert.assertEquals(stargateRequest.getAttachments().size(), 0);

    }

}