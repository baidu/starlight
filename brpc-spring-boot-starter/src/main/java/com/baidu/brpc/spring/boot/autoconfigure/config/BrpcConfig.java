package com.baidu.brpc.spring.boot.autoconfigure.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BrpcConfig {
    private BrpcNamingConfig naming;
    private RpcClientConfig client;
    private RpcServerConfig server;
}
