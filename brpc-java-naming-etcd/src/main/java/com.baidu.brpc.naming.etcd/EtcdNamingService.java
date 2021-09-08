package com.baidu.brpc.naming.etcd;

import com.baidu.brpc.client.channel.Endpoint;
import com.baidu.brpc.client.channel.ServiceInstance;
import com.baidu.brpc.naming.*;
import com.baidu.brpc.protocol.SubscribeInfo;
import com.baidu.brpc.utils.GsonUtils;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.options.GetOption;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class EtcdNamingService extends FailbackNamingService implements NamingService {

    protected Client client;
    private int ttlMills;
    private int connectTimeoutMs;

    public EtcdNamingService(BrpcURL url) {
        super(url);
        String namespace = Constants.DEFAULT_PATH;
        if (url.getPath().startsWith("/")) {
            namespace = url.getPath().substring(1);
        }
        client = Client.builder()
                .endpoints("http://".concat(namespace))
                .build();
    }

    @Override
    public List<ServiceInstance> lookup(SubscribeInfo subscribeInfo) {
        List<ServiceInstance> instances = new ArrayList<>();
        String prefixKey = getSubscribeKey(subscribeInfo);
        KV kvClient = client.getKVClient();
        GetOption getOption = GetOption.newBuilder()
                .withPrefix(ByteSequence.from(prefixKey.getBytes(StandardCharsets.UTF_8)))
                .build();
        List<KeyValue> keyValues = null;
        try {
            keyValues =  kvClient.get(
                    ByteSequence.from(prefixKey.getBytes(StandardCharsets.UTF_8)),getOption)
                    .get()
                    .getKvs();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        for(KeyValue keyValue : keyValues){
            ByteSequence value = keyValue.getValue();
            Endpoint endpoint = GsonUtils.fromJson(value.toString(StandardCharsets.UTF_8), Endpoint.class);
            ServiceInstance instance = new ServiceInstance(endpoint);
            instances.add(instance);
        }

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

    private String getSubscribeKey(SubscribeInfo subscribeInfo) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(subscribeInfo.getGroup()).append("_");
        stringBuilder.append(subscribeInfo.getInterfaceName()).append("_");
        stringBuilder.append(subscribeInfo.getVersion());
        return null;
    }


}
