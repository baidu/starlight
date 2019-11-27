package com.baidu.brpc;

import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.protocol.Options;
import com.baidu.brpc.server.RpcServerOptions;
import org.junit.Assert;
import org.junit.Test;

public class RpcOptionsTest {

  @Test
  public void testRpcClientOptionsBuilderMode() {
    RpcClientOptions client = RpcClientOptions.builder()
          .encoding("UTF-8")
          .protocolType(Options.ProtocolType.PROTOCOL_BAIDU_STD_VALUE)
          .healthyCheckIntervalMillis(1000)
          .reuseAddr(true)
          .clientName("client")
          .build();

    Assert.assertEquals("UTF-8",client.getEncoding());
    Assert.assertEquals(Options.ProtocolType.PROTOCOL_BAIDU_STD_VALUE,client.getProtocolType());
    Assert.assertEquals(1000,client.getHealthyCheckIntervalMillis());
    Assert.assertEquals(true,client.isReuseAddr());
    Assert.assertEquals("client",client.getClientName());
  }

  @Test
  public void testRpcServerOptionsBuilderMode() {
    RpcServerOptions server = RpcServerOptions.builder()
          .encoding("UTF-8")
          .keepAlive(false)
          .maxSize(2000)
          .build();

    Assert.assertEquals("UTF-8",server.getEncoding());
    Assert.assertEquals(false,server.isKeepAlive());
    Assert.assertEquals(2000,server.getMaxSize());
  }

}
