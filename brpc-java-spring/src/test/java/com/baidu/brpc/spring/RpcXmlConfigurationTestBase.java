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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Base RPC XML configuration test class.
 *
 * @author xiemalin
 * @since 2.17
 */
public abstract class RpcXmlConfigurationTestBase {

    /**
     * context of {@link AbstractApplicationContext}
     */
    protected AbstractApplicationContext context;
    


    @Before
    public void setUp() {
        context =
                new ClassPathXmlApplicationContext(getConfigurationPath());
    }
    
    protected abstract String getConfigurationPath();

    @After
    public void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    protected void internalRpcRequestAndResponse(EchoService echoService) {
        EchoRequest echo = new EchoRequest();
        echo.setMessage("world");

        EchoResponse response = echoService.echo(echo);
        Assert.assertEquals("world", response.getMessage());
    }
}
