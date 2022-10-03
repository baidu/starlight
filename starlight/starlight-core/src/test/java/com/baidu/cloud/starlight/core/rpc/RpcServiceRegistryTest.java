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
 
package com.baidu.cloud.starlight.core.rpc;

import com.baidu.cloud.starlight.api.rpc.RpcService;
import com.baidu.cloud.starlight.api.rpc.ServiceInvoker;
import com.baidu.cloud.starlight.core.integrate.service.UserService;
import com.baidu.cloud.starlight.core.integrate.service.UserServiceImpl;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by liuruisen on 2020/3/23.
 */
public class RpcServiceRegistryTest {

    @Test
    public void registerUnRegister() {
        RpcServiceRegistry registry = RpcServiceRegistry.getInstance();
        registry.destroy();
        ServiceInvoker serviceInvoker = new RpcServiceInvoker(new RpcService(UserService.class, new UserServiceImpl()));

        registry.unRegister(serviceInvoker);
        Assert.assertEquals(registry.rpcServices().size(), 0);

        registry.register(serviceInvoker);
        Assert.assertEquals(registry.rpcServices().size(), 1);

        registry.unRegister(serviceInvoker);
        Assert.assertEquals(registry.rpcServices().size(), 0);
    }

    @Test
    public void discover() {
        RpcServiceRegistry registry = RpcServiceRegistry.getInstance();
        registry.destroy();
        ServiceInvoker serviceInvoker = new RpcServiceInvoker(new RpcService(UserService.class, new UserServiceImpl()));
        registry.unRegister(serviceInvoker);
        registry.register(serviceInvoker);
        Assert.assertEquals(registry.rpcServices().size(), 1);

        ServiceInvoker result =
            registry.discover(new RpcService(UserService.class, new UserServiceImpl()).getServiceName());
        Assert.assertEquals(result.getRpcService().getServiceName(), UserService.class.getName());
    }

    @Test
    public void rpcServices() {
        RpcServiceRegistry registry = RpcServiceRegistry.getInstance();
        registry.destroy();
        ServiceInvoker serviceInvoker = new RpcServiceInvoker(new RpcService(UserService.class, new UserServiceImpl()));
        registry.unRegister(serviceInvoker);
        registry.register(serviceInvoker);

        Assert.assertEquals(registry.rpcServices().size(), 1);
    }

    @Test
    public void destroy() {
        RpcServiceRegistry registry = RpcServiceRegistry.getInstance();
        registry.destroy();
        Assert.assertEquals(registry.rpcServices().size(), 0);
    }
}