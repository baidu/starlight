package com.baidu.brpc.naming.etcd;

import com.baidu.brpc.client.channel.ServiceInstance;
import com.baidu.brpc.naming.*;
import com.baidu.brpc.protocol.SubscribeInfo;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;

import java.util.ArrayList;
import java.util.List;

public class EtcdNamingService extends FailbackNamingService implements NamingService {

    protected Client client;

    public EtcdNamingService(BrpcURL url) {
        super(url);
        String namespace = Constants.DEFAULT_PATH;
        if (url.getPath().startsWith("/")) {
            namespace = url.getPath().substring(1);
        }
        client = Client.builder().endpoints(namespace).build();
    }

    @Override
    public List<ServiceInstance> lookup(SubscribeInfo subscribeInfo) {

        List<ServiceInstance> instances = new ArrayList<ServiceInstance>();
        KV kvClient = client.getKVClient();


        return instances;
    }

    @Override
    public void doSubscribe(SubscribeInfo subscribeInfo, NotifyListener listener) throws Exception {

    }

    @Override
    public void doUnsubscribe(SubscribeInfo subscribeInfo) throws Exception {

    }

    @Override
    public void doRegister(RegisterInfo registerInfo) throws Exception {

        KV kvClient = client.getKVClient();

    }

    @Override
    public void doUnregister(RegisterInfo registerInfo) throws Exception {

    }


}
