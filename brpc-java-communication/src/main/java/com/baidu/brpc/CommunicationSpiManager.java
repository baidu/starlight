package com.baidu.brpc;

import com.baidu.brpc.protocol.ProtocolFactory;
import com.baidu.brpc.protocol.ProtocolManager;

import java.util.*;

public class CommunicationSpiManager {
    private static volatile CommunicationSpiManager instance;

    public static CommunicationSpiManager getInstance() {
        if (instance == null) {
            synchronized (CommunicationSpiManager.class) {
                if (instance == null) {
                    instance = new CommunicationSpiManager();
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
                    loadProtocol(encoding);
                    isLoaded = true;
                }
            }
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

}
