package com.baidu.brpc;

import com.baidu.brpc.loadbalance.LoadBalanceFactory;
import com.baidu.brpc.loadbalance.LoadBalanceManager;
import com.baidu.brpc.naming.NamingServiceFactory;
import com.baidu.brpc.naming.NamingServiceFactoryManager;

import java.util.*;

public class GovernanceSpiManager {
    private static volatile GovernanceSpiManager instance;

    public static GovernanceSpiManager getInstance() {
        if (instance == null) {
            synchronized (GovernanceSpiManager.class) {
                if (instance == null) {
                    instance = new GovernanceSpiManager();
                }
            }
        }
        return instance;
    }

    private Object loadLock = new Object();
    private Boolean isLoaded = false;

    /**
     * load all extensions with java spi
     */
    public void loadAllExtensions() {
        if (!isLoaded) {
            synchronized (loadLock) {
                if (!isLoaded) {
                    loadNamingService();
                    loadLoadBalance();
                    isLoaded = true;
                }
            }
        }
    }

    public void loadNamingService() {
        NamingServiceFactoryManager manager = NamingServiceFactoryManager.getInstance();
        ServiceLoader<NamingServiceFactory> namingServiceFactories = ServiceLoader.load(NamingServiceFactory.class);
        for (NamingServiceFactory namingServiceFactory : namingServiceFactories) {
            manager.registerNamingServiceFactory(namingServiceFactory);
        }
    }

    public void loadLoadBalance() {
        LoadBalanceManager loadBalanceManager = LoadBalanceManager.getInstance();
        ServiceLoader<LoadBalanceFactory> loadBalanceFactories = ServiceLoader.load(LoadBalanceFactory.class);
        for (LoadBalanceFactory loadBalanceFactory : loadBalanceFactories) {
            loadBalanceManager.registerLoadBalanceFactory(loadBalanceFactory);
        }
    }

}
