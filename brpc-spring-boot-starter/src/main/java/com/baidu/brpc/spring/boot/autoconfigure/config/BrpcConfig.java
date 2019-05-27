package com.baidu.brpc.spring.boot.autoconfigure.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BrpcConfig {
    private RpcNamingConfig naming;
    private RpcClientConfig client;
    private RpcServerConfig server;

    public BrpcConfig() {
    }

    public BrpcConfig(BrpcConfig rhs) {
        if (rhs.getNaming() != null) {
            this.naming = new RpcNamingConfig(rhs.getNaming());
        }
        if (rhs.getClient() != null) {
            this.client = new RpcClientConfig(rhs.getClient());
        }
        if (rhs.getServer() != null) {
            this.server = new RpcServerConfig(rhs.getServer());
        }
    }
}
