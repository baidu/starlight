package com.baidu.brpc;

import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.protocol.Options;
import com.baidu.brpc.server.RpcServerOptions;
import org.junit.Assert;
import org.junit.Test;

public class RpcOptionsTest {

  @Test
  public void testRpcClientOptionsBuilderMode() {
    RpcClientOptions clientOptions = RpcClientOptions.builder()
          .encoding("UTF-8")
          .protocolType(Options.ProtocolType.PROTOCOL_BAIDU_STD_VALUE)
          .healthyCheckIntervalMillis(1000)
          .reuseAddr(true)
          .clientName("client")
          .build();

    Assert.assertEquals("UTF-8", clientOptions.getEncoding());
    Assert.assertEquals(Options.ProtocolType.PROTOCOL_BAIDU_STD_VALUE, clientOptions.getProtocolType());
    Assert.assertEquals(1000, clientOptions.getHealthyCheckIntervalMillis());
    Assert.assertEquals(true, clientOptions.isReuseAddr());
    Assert.assertEquals("client", clientOptions.getClientName());
  }

  @Test
  public void testRpcServerOptionsBuilderMode() {
    RpcServerOptions serverOptions = RpcServerOptions.builder()
          .encoding("UTF-8")
          .keepAlive(false)
          .maxSize(2000)
          .build();

    Assert.assertEquals("UTF-8", serverOptions.getEncoding());
    Assert.assertEquals(false, serverOptions.isKeepAlive());
    Assert.assertEquals(2000, serverOptions.getMaxSize());
  }

}
