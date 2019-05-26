package com.baidu.brpc.spring.boot.autoconfigure.config;

import com.baidu.brpc.client.RpcClientOptions;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RpcClientConfig extends RpcClientOptions {
    private String interceptorBeanName;
}
