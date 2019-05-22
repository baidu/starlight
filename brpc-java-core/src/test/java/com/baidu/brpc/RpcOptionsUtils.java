package com.baidu.brpc;

import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.server.RpcServerOptions;

public class RpcOptionsUtils {
    public static RpcClientOptions getRpcClientOptions() {
        RpcClientOptions options = new RpcClientOptions();
        options.setIoThreadNum(1);
        options.setWorkThreadNum(1);
        options.setMinIdleConnections(1);
        return options;
    }

    public static RpcServerOptions getRpcServerOptions() {
        RpcServerOptions options = new RpcServerOptions();
        options.setIoThreadNum(1);
        options.setWorkThreadNum(1);
        return options;
    }
}
