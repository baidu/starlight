package com.baidu.brpc.spi;

import com.baidu.brpc.client.loadbalance.LoadBalanceFactory;
import com.baidu.brpc.client.loadbalance.LoadBalanceManager;
import com.baidu.brpc.naming.NamingServiceFactory;
import com.baidu.brpc.naming.NamingServiceFactoryManager;
import com.baidu.brpc.protocol.ProtocolFactory;
import com.baidu.brpc.protocol.ProtocolManager;

import java.util.*;

public class ExtensionLoaderManager {
    private static volatile ExtensionLoaderManager instance;

    public static ExtensionLoaderManager getInstance() {
        if (instance == null) {
            synchronized (ExtensionLoaderManager.class) {
                if (instance == null) {
                    instance = new ExtensionLoaderManager();
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
    public void loadAllExtensions(String encoding) {
        if (!isLoaded) {
            synchronized (loadLock) {
                if (!isLoaded) {
                    loadNamingService();
                    loadProtocol(encoding);
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

    public void loadProtocol(String encoding) {
        ProtocolManager protocolManager = ProtocolManager.getInstance();
        ServiceLoader<ProtocolFactory> protocolFactories = ServiceLoader.load(ProtocolFactory.class);
        List<ProtocolFactory> protocolFactoryList = new ArrayList<ProtocolFactory>();
        for (ProtocolFactory protocolFactory : protocolFactories) {
            protocolFactoryList.add(protocolFactory);
        }
        Collections.sort(protocolFactoryList, new Comparator<ProtocolFactory>() {
            @Override
            public int compare(ProtocolFactory o1, ProtocolFactory o2) {
                return o1.getPriority() - o2.getPriority();
            }
        });
        for (ProtocolFactory protocolFactory : protocolFactoryList) {
            protocolManager.registerProtocol(protocolFactory, encoding);
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
