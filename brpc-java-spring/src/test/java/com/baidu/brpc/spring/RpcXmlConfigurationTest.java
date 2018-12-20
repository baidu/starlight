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

import org.junit.Test;


/**
 * Test class for {@link RpcServiceExporter} and {@link RpcProxyFactoryBean} by XML configuration.
 * 
 * @author xiemalin
 * @since 2.17
 */

public class RpcXmlConfigurationTest extends RpcXmlConfigurationTestBase {


    /*
     * (non-Javadoc)
     * 
     * @see com.baidu.jprotobuf.pbrpc.spring.RpcXmlConfigurationTestBase#getConfigurationPath()
     */
    @Override
    protected String getConfigurationPath() {
        return "classpath:" + RpcXmlConfigurationTest.class.getName().replace('.', '/') + ".xml";
    }
    
    
    @Test
    public void testRpcRequestAndResponse() {
        EchoService echoService = (EchoService) context.getBean("echoServiceProxy", EchoService.class);
        super.internalRpcRequestAndResponse(echoService);
    }
}
