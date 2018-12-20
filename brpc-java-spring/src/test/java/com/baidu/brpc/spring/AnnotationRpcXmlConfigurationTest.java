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
import org.junit.Test;

/**
 * 
 * 
 * @author xiemalin
 * @since 2.17
 */
public class AnnotationRpcXmlConfigurationTest extends RpcXmlConfigurationTestBase {

    protected String getConfigurationPath() {
        return "classpath:" + AnnotationRpcXmlConfigurationTest.class.getName().replace('.', '/') + ".xml";
    }

    @Test
    public void testCommonRpcRequest() {

        AnnotationEchoServiceClient annotationEchoServiceClient =
                (AnnotationEchoServiceClient) context.getBean("echoServiceClient", AnnotationEchoServiceClient.class);
        
        // test common client
        super.internalRpcRequestAndResponse(annotationEchoServiceClient.getEchoService());

    }
    
    @Test
    public void testHaRpcRequest() {

        AnnotationEchoServiceClient annotationEchoServiceClient =
                (AnnotationEchoServiceClient) context.getBean("echoServiceClient", AnnotationEchoServiceClient.class);
        
        // test ha client
        super.internalRpcRequestAndResponse(annotationEchoServiceClient.getEchoService());
        
    }
    
    @Test
    public void testHaRpcRequestWithPartialFailed() {

        AnnotationEchoServiceClient annotationEchoServiceClient =
                (AnnotationEchoServiceClient) context.getBean("echoServiceClient", AnnotationEchoServiceClient.class);
        
        // test ha client
        super.internalRpcRequestAndResponse(annotationEchoServiceClient.getEchoService());
    }
    
    protected void internalRpcRequestAndResponseTimeout(EchoService echoService) {
        EchoRequest echo = new EchoRequest();
        echo.setMessage("world");

        EchoResponse response = echoService.echo(echo);
        Assert.assertEquals("world", response.getMessage());
    }
}
