package com.baidu.cloud.starlight.benchmark;

import com.baidu.cloud.starlight.benchmark.service.UserService;
import com.baidu.cloud.starlight.core.rpc.DefaultStarlightServer;
import com.baidu.cloud.starlight.api.rpc.StarlightServer;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.api.rpc.config.TransportConfig;
import com.baidu.cloud.starlight.benchmark.service.UserServiceImpl;

/**
 * Created by liuruisen on 2020/3/11.
 */
public class StarlightProvider {

    public static void main(String[] args) {
        // init starlight server
        TransportConfig transportConfig = new TransportConfig();
        transportConfig.setIoThreadNum(8);
        transportConfig.setAcceptThreadNum(1);
        transportConfig.setWriteTimeoutMills(3000);
        transportConfig.setAllIdleTimeout(4000);
        StarlightServer starlightServer = new DefaultStarlightServer("localhost", 8005, transportConfig);
        starlightServer.init();

        // export service
        // ServiceConfig serviceConfig = new ServiceConfig();
        // starlightServer.export(UserService.class, new UserServiceImpl());
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setThreadPoolSize(8);
        serviceConfig.setMaxThreadPoolSize(10);
        serviceConfig.setMaxRunnableQueueSize(2048);
        serviceConfig.setIdleThreadKeepAliveSecond(10);
        starlightServer.export(UserService.class, new UserServiceImpl(), serviceConfig);
        // serve
        starlightServer.serve();


        synchronized (StarlightProvider.class) {
            try {
                StarlightProvider.class.wait();
            } catch (Throwable e) {
            }
        }
    }
}
