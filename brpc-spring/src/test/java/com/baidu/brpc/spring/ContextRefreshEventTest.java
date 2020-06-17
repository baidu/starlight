package com.baidu.brpc.spring;

import org.junit.Test;

/**
 * created by wangsan on 2020/6/16.
 *
 * @author wangsan
 */
public class ContextRefreshEventTest extends RpcXmlConfigurationTestBase {

    protected String getConfigurationPath() {
        return "classpath:" + ContextRefreshEventTest.class.getName().replace('.', '/') + ".xml";
    }

    @Test
    public void testCommonRpcRequest() {

        AnnotationEchoServiceClient annotationEchoServiceClient =
                (AnnotationEchoServiceClient) context.getBean("echoServiceClient", AnnotationEchoServiceClient.class);

        // test common client
        super.internalRpcRequestAndResponse(annotationEchoServiceClient.getEchoService());

    }


}
