package com.baidu.brpc.spring.boot.autoconfigure.config;

import com.baidu.brpc.naming.NamingOptions;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RpcNamingConfig extends NamingOptions {

    private String namingServiceUrl;

    private String namingServiceFactory;

    public RpcNamingConfig() {
    }

    public RpcNamingConfig(RpcNamingConfig rhs) {
        super(rhs);
        this.namingServiceUrl = rhs.getNamingServiceUrl();
        this.namingServiceFactory = rhs.getNamingServiceFactory();
    }
}
