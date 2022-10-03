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

import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.api.service.UserService;
import com.baidu.cloud.starlight.api.service.UserServiceImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by liuruisen on 2020/3/23.
 */
public class RpcServiceTest {

    private RpcService rpcService;

    @Before
    public void before() {
        rpcService = new RpcService(UserService.class, new UserServiceImpl());
    }

    @Test
    public void getServiceName() {
        Assert.assertTrue(rpcService.getServiceName().contains(UserService.class.getName()));
    }

    @Test
    public void getServiceInterface() {
        Assert.assertEquals(rpcService.getServiceClass(), UserService.class);
    }

    @Test
    public void getServiceObject() {
        Assert.assertTrue(rpcService.getServiceObject() instanceof UserServiceImpl);
    }

    @Test
    public void getMethod() {
        Assert.assertNotNull(rpcService.getMethod("getUser"));
    }

    @Test
    public void getServiceConfig() {
        rpcService.setServiceConfig(new ServiceConfig());
        Assert.assertNotNull(rpcService.getServiceConfig());
    }
}