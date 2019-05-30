package com.baidu.brpc.naming;

import java.util.HashMap;
import java.util.Map;

public class NamingServiceFactoryManager {
    private static volatile NamingServiceFactoryManager instance;

    private Map<String, NamingServiceFactory> namingServiceFactoryMap;

    public static NamingServiceFactoryManager getInstance() {
        if (instance == null) {
            synchronized (NamingServiceFactoryManager.class) {
                if (instance == null) {
                    instance = new NamingServiceFactoryManager();
                }
            }
        }
        return instance;
    }

    private NamingServiceFactoryManager() {
        this.namingServiceFactoryMap = new HashMap<String, NamingServiceFactory>();
        this.namingServiceFactoryMap.put("list", new ListNamingServiceFactory());
        this.namingServiceFactoryMap.put("file", new FileNamingServiceFactory());
        this.namingServiceFactoryMap.put("dns", new DnsNamingServiceFactory());
    }

    public void registerNamingServiceFactory(NamingServiceFactory namingServiceFactory) {
        if (namingServiceFactoryMap.get(namingServiceFactory.getName()) != null) {
            throw new RuntimeException("naming service exist:" + namingServiceFactory.getName());
        }
        namingServiceFactoryMap.put(namingServiceFactory.getName(), namingServiceFactory);
    }

    public NamingServiceFactory getNamingServiceFactory(String name) {
        return namingServiceFactoryMap.get(name);
    }

}
