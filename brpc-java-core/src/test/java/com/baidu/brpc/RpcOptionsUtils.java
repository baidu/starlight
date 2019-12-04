package com.baidu.brpc;

import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.server.RpcServerOptions;

public class RpcOptionsUtils {
    public static RpcClientOptions getRpcClientOptions() {
        return RpcClientOptions.builder()
              .ioThreadNum(1)
              .workThreadNum(1)
              .minIdleConnections(1)
              .build();
    }

    public static RpcServerOptions getRpcServerOptions() {
        return RpcServerOptions.builder()
              .ioThreadNum(1)
              .workThreadNum(1)
              .build();
    }
}
