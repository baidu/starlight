package com.baidu.brpc.spi;

import com.baidu.brpc.naming.NamingServiceFactory;
import com.baidu.brpc.naming.NamingServiceFactoryManager;
import com.baidu.brpc.protocol.ProtocolFactory;
import com.baidu.brpc.protocol.ProtocolManager;

import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private AtomicBoolean load = new AtomicBoolean(false);

    /**
     * load all extensions with java spi
     */
    public void loadAllExtensions(String encoding) {
        if (load.compareAndSet(false, true)) {
            loadNamingService();
            loadProtocol(encoding);
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
        for (ProtocolFactory protocolFactory : protocolFactories) {
            protocolManager.registerProtocol(protocolFactory, encoding);
        }
    }
}
