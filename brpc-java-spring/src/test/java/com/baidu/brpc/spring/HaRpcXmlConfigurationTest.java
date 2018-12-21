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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for {@link RpcServiceExporter} and {@link RpcProxyFactoryBean} by XML configuration.
 * 
 * @author xiemalin
 * @since 2.17
 */

public class HaRpcXmlConfigurationTest extends RpcXmlConfigurationTestBase {

    private RpcServiceExporter rpcServiceExporter1;
    private RpcServiceExporter rpcServiceExporter2;
    private RpcServiceExporter rpcServiceExporter3;

    /*
     * (non-Javadoc)
     * 
     * @see com.baidu.jprotobuf.pbrpc.spring.RpcXmlConfigurationTestBase#getConfigurationPath()
     */
    @Override
    protected String getConfigurationPath() {
        return "classpath:" + HaRpcXmlConfigurationTest.class.getName().replace('.', '/') + ".xml";
    }

    @Before
    public void setUp() {
        super.setUp();
        rpcServiceExporter1 = (RpcServiceExporter) context.getBean("rpcServer1", RpcServiceExporter.class);
        rpcServiceExporter2 = (RpcServiceExporter) context.getBean("rpcServer2", RpcServiceExporter.class);
        rpcServiceExporter3 = (RpcServiceExporter) context.getBean("rpcServer3", RpcServiceExporter.class);
    }

    @Test
    public void testPartialServerFailed() throws Exception {
        
        EchoService echoService = (EchoService) context.getBean("echoServiceProxy");
        
        // shutdown server1
        if (rpcServiceExporter1 != null) {
            rpcServiceExporter1.destroy();
        }

        super.internalRpcRequestAndResponse(echoService);

        // shutdown server2
        if (rpcServiceExporter2 != null) {
            rpcServiceExporter2.destroy();
        }
        super.internalRpcRequestAndResponse(echoService);
        
        // shutdown all servers
        if (rpcServiceExporter3 != null) {
            rpcServiceExporter3.destroy();
        }
        
        try {
            super.internalRpcRequestAndResponse(echoService);
            // should throw exception on no servers available
            Assert.fail("No servers available should throw exception");
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
        
        // recover server1
        if (rpcServiceExporter1 != null) {
            rpcServiceExporter1.afterPropertiesSet();
        }

        Thread.sleep(5000);
        // server1 recover should test ok
        super.internalRpcRequestAndResponse(echoService);
        
    }
}
