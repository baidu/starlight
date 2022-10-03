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

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by liuruisen on 2020/7/27.
 */
public class StargateResponseTest {

    @Test
    public void responseTest() {
        StargateResponse stargateResponse = new StargateResponse("1");
        stargateResponse.setId("2");
        stargateResponse.setResult("Test");

        Assert.assertEquals(stargateResponse.getId(), "2");
        Assert.assertEquals(stargateResponse.getResult(), "Test");
        Assert.assertNull(stargateResponse.getAttachments());
        Assert.assertNull(stargateResponse.getAttachment("Key"));
        stargateResponse.setAttachment("Key", "Value");
        Assert.assertEquals(stargateResponse.getAttachment("Key"), "Value");
        stargateResponse.setException(new Exception());
        Assert.assertTrue(stargateResponse.hasException());
        Assert.assertTrue(stargateResponse.getException() instanceof Exception);

        StargateResponse stargateResponse1 = new StargateResponse("1", null, new Exception());
        stargateResponse1.setAttachments(null);
        Assert.assertEquals(stargateResponse1.getAttachments().size(), 0);
    }
}