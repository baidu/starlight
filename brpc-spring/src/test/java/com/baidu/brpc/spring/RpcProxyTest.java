/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baidu.brpc.spring;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 * Test for {@link RpcProxyFactoryBean} and {@link RpcServiceExporter}
 *
 * @author xiemalin
 * @since 2.1.0.0
 */

public class RpcProxyTest {
    
    private int servicePort = 1031;

    private RpcProxyFactoryBean rpcProxyFactoryBean;
    
    private RpcServiceExporter rpcServiceExporter;
    
    @Before
    public void setUp() throws Exception {
        
        rpcServiceExporter = new RpcServiceExporter();
        rpcServiceExporter.setIoThreadNum(1);
        rpcServiceExporter.setWorkThreadNum(1);
        rpcServiceExporter.setServicePort(servicePort);
        
        EchoServiceImpl service = new EchoServiceImpl();
        rpcServiceExporter.setRegisterServices(new ArrayList<Object>(Arrays.asList(service)));
        
        rpcServiceExporter.afterPropertiesSet();
        
        // setup client
        rpcProxyFactoryBean = new RpcProxyFactoryBean();
        rpcProxyFactoryBean.setIoThreadNum(1);
        rpcProxyFactoryBean.setWorkThreadNum(1);
        rpcProxyFactoryBean.setServiceInterface(EchoService.class);
        rpcProxyFactoryBean.setNamingServiceUrl("list://127.0.0.1:" + servicePort);
        rpcProxyFactoryBean.afterPropertiesSet();
        
    }
    
    @After
    public void tearDown() {
        try {
            if (rpcProxyFactoryBean != null) {
                rpcProxyFactoryBean.destroy();
            }
            if (rpcServiceExporter != null) {
                rpcServiceExporter.destroy();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Test
    public void testClientSend() throws Exception {
        Object object = rpcProxyFactoryBean.getObject();
        Assert.assertTrue(object instanceof EchoService);
        
        EchoService echoService = (EchoService) object;
        
        EchoRequest echo = new EchoRequest();
        echo.setMessage("world");
        
        EchoResponse response = echoService.echo(echo);
        Assert.assertEquals("world", response.getMessage());
        
    }
}
