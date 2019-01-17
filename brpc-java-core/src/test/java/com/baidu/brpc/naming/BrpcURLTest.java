/*
 * Copyright (c) 2018 Baidu, Inc. All Rights Reserved.
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

package com.baidu.brpc.naming;

import org.junit.Assert;
import org.junit.Test;

public class BrpcURLTest {
    @Test
    public void testBns() {
        String bns = "bns://test.com";
        BrpcURL uri = new BrpcURL(bns);
        Assert.assertTrue(uri.getSchema().equals("bns"));
        Assert.assertTrue(uri.getHostPorts().equals("test.com"));
    }

    @Test
    public void testList() {
        String serviceUrl = "list://127.0.0.1:8002,127.0.0.1:8003";
        BrpcURL uri = new BrpcURL(serviceUrl);
        Assert.assertTrue(uri.getSchema().equals("list"));
        Assert.assertTrue(uri.getHostPorts().equals("127.0.0.1:8002,127.0.0.1:8003"));
    }

    @Test
    public void testFile() {
        String serviceUrl = "file:///home/ubuntu/test.config";
        BrpcURL uri = new BrpcURL(serviceUrl);
        Assert.assertTrue(uri.getSchema().equals("file"));
        Assert.assertTrue(uri.getHostPorts().equals(""));
        Assert.assertTrue(uri.getPath().equals("/home/ubuntu/test.config"));
    }

    @Test
    public void testDns() {
        String serviceUrl = "dns://weibo.com";
        BrpcURL uri = new BrpcURL(serviceUrl);
        Assert.assertTrue(uri.getSchema().equals("dns"));
        Assert.assertTrue(uri.getHostPorts().equals("weibo.com"));
    }
}
