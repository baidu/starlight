package com.baidu.brpc.spring.boot.autoconfigure.config;

import com.baidu.brpc.server.RpcServerOptions;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RpcServerConfig extends RpcServerOptions {
    private int port;
    private boolean useSharedThreadPool;
    private String interceptorBeanName;

    public RpcServerConfig() {
    }

    public RpcServerConfig(RpcServerConfig rhs) {
        super(rhs);
        this.port = rhs.getPort();
        this.useSharedThreadPool = rhs.isUseSharedThreadPool();
        this.interceptorBeanName = rhs.getInterceptorBeanName();
    }
}
