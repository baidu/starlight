package com.baidu.brpc;

import com.baidu.brpc.client.CommunicationOptions;
import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.server.RpcServerOptions;

import java.util.ArrayList;
import java.util.List;

public class RpcOptionsUtils {
    public static RpcClientOptions getRpcClientOptions() {
        RpcClientOptions options = new RpcClientOptions();
        options.setIoThreadNum(1);
        options.setWorkThreadNum(1);
        options.setMinIdleConnections(0);
        return options;
    }

    public static CommunicationOptions getCommunicationOptions() {
        return getRpcClientOptions().buildCommunicationOptions(null);
    }

    public static RpcServerOptions getRpcServerOptions() {
        RpcServerOptions options = new RpcServerOptions();
        options.setIoThreadNum(1);
        options.setWorkThreadNum(1);
        return options;
    }
}
