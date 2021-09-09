package com.baidu.brpc.naming.etcd;

import com.baidu.brpc.client.channel.Endpoint;
import com.baidu.brpc.client.channel.ServiceInstance;
import com.baidu.brpc.naming.*;
import com.baidu.brpc.naming.Constants;
import com.baidu.brpc.protocol.SubscribeInfo;
import com.baidu.brpc.utils.GsonUtils;
import io.etcd.jetcd.*;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import io.grpc.stub.StreamObserver;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class EtcdNamingService extends FailbackNamingService implements NamingService {

    protected Client client;
    private int ttlSec;
    private int connectTimeoutMs;

    public EtcdNamingService(BrpcURL url) {
        super(url);
        String namespace = Constants.DEFAULT_PATH;
        if (url.getPath().startsWith("/")) {
            namespace = url.getPath().substring(1);
        }
        client = Client.builder()
                .endpoints(namespace).build();
        ttlSec = url.getIntParameter(
                Constants.SESSION_TIMEOUT_MS, Constants.DEFAULT_SESSION_TIMEOUT_MS)/1000;
        connectTimeoutMs = url.getIntParameter(
                Constants.CONNECT_TIMEOUT_MS, Constants.DEFAULT_CONNECT_TIMEOUT_MS);
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
                    .get(connectTimeoutMs, TimeUnit.MILLISECONDS)
                    .getKvs();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
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
        ByteSequence registerKey = ByteSequence.from(
                getRegisterKey(registerInfo).getBytes(StandardCharsets.UTF_8));
        ByteSequence registerValue = ByteSequence.from(
                getRegisterValue(registerInfo).getBytes(StandardCharsets.UTF_8));
        KV kvClient = client.getKVClient();
        Lease leaseClient = client.getLeaseClient();
        PutOption putOption = PutOption.newBuilder()
                .withLeaseId(leaseClient.grant(ttlSec).get(connectTimeoutMs, TimeUnit.MILLISECONDS).getID())
                .build();
        kvClient.put(registerKey,registerValue,putOption);
        leaseClient.keepAlive(putOption.getLeaseId(), new StreamObserver<LeaseKeepAliveResponse>() {
            @Override
            public void onNext(LeaseKeepAliveResponse leaseKeepAliveResponse) {

            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {

            }
        });
    }


    @Override
    public void doUnregister(RegisterInfo registerInfo) throws Exception {

    }

    private String getRegisterKey(RegisterInfo registerInfo) {
        StringBuilder registerKey = new StringBuilder();
        registerKey.append(registerInfo.getGroup()).append("_");
        registerKey.append(registerInfo.getInterfaceName()).append("_");
        registerKey.append(registerInfo.getVersion()).append("_");
        registerKey.append(registerInfo.getHost());
        return registerKey.toString();
    }

    private String getSubscribeKey(SubscribeInfo subscribeInfo) {
        StringBuilder subscribeKey = new StringBuilder();
        subscribeKey.append(subscribeInfo.getGroup()).append("_");
        subscribeKey.append(subscribeInfo.getInterfaceName()).append("_");
        subscribeKey.append(subscribeInfo.getVersion());
        return subscribeKey.toString();
    }

    public String getRegisterValue(RegisterInfo registerInfo) {
        Endpoint endPoint = new Endpoint(registerInfo.getHost(), registerInfo.getPort());
        return GsonUtils.toJson(endPoint);
    }


}
