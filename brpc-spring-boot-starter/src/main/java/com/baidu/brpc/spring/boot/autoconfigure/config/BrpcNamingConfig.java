package com.baidu.brpc.spring.boot.autoconfigure.config;

import com.baidu.brpc.naming.NamingOptions;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BrpcNamingConfig extends NamingOptions {

    private String namingServiceUrl;

    private String namingServiceFactory;
}
