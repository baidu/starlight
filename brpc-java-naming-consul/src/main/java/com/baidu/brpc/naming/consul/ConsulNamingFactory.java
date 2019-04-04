package com.baidu.brpc.naming.consul;

import com.baidu.brpc.naming.BrpcURL;
import com.baidu.brpc.naming.NamingService;
import com.baidu.brpc.naming.NamingServiceFactory;

public class ConsulNamingFactory implements NamingServiceFactory {
    @Override
    public NamingService createNamingService(BrpcURL url) {
        return new ConsulNamingService(url);
    }
}
