package com.baidu.brpc.naming.etcd;

import com.baidu.brpc.naming.BrpcURL;
import com.baidu.brpc.naming.NamingService;
import com.baidu.brpc.naming.NamingServiceFactory;

public class EtcdNamingFactory implements NamingServiceFactory {
    @Override
    public String getName() {
        return "etcd";
    }

    @Override
    public NamingService createNamingService(BrpcURL url) {
        return new EtcdNamingService(url);
    }
}
