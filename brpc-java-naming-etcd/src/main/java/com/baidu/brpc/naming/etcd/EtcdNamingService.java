package com.baidu.brpc.naming.etcd;

import com.baidu.brpc.client.channel.Endpoint;
import com.baidu.brpc.client.channel.ServiceInstance;
import com.baidu.brpc.naming.Constants;
import com.baidu.brpc.naming.*;
import com.baidu.brpc.protocol.SubscribeInfo;
import com.baidu.brpc.utils.GsonUtils;
import io.etcd.jetcd.*;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;
import io.etcd.jetcd.watch.WatchResponse;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class EtcdNamingService extends FailbackNamingService implements NamingService {

    protected Client client;
    private int ttlSec;
    private int connectTimeoutMs;
    private ConcurrentHashMap<ByteSequence,Long> leaseMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<SubscribeInfo,Watch> watchMap = new ConcurrentHashMap<>();

    public EtcdNamingService(BrpcURL url) {
        super(url);
        client = Client.builder()
                .endpoints("http://".concat(url.getHostPorts())).build();
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
            log.warn("lookup interrupted, key:{}, msg={}",prefixKey,e.getMessage());
        } catch (ExecutionException e) {
            log.warn("lookup failed, key:{}, msg={}",prefixKey,e.getMessage());
        } catch (TimeoutException e) {
            log.warn("lookup timeout, key:{}, msg={}",prefixKey,e.getMessage());
        }
        for(KeyValue keyValue : keyValues){
            ByteSequence value = keyValue.getValue();
            Endpoint endpoint = GsonUtils.fromJson(value.toString(StandardCharsets.UTF_8), Endpoint.class);
            ServiceInstance instance = new ServiceInstance(endpoint);
            instance.setServiceName(subscribeInfo.getInterfaceName());
            instances.add(instance);
        }
        return instances;
    }

    @Override
    public void doSubscribe(SubscribeInfo subscribeInfo, NotifyListener listener) throws Exception {
        Watch watchClient = client.getWatchClient();
        String prefixKey = getSubscribeKey(subscribeInfo);
        WatchOption watchOption = WatchOption.newBuilder()
                .withPrefix(ByteSequence.from(prefixKey.getBytes(StandardCharsets.UTF_8)))
                        .build();
        watchClient.watch(ByteSequence.from(prefixKey.getBytes(StandardCharsets.UTF_8)), watchOption,new Watch.Listener() {
            @Override
            public void onNext(WatchResponse response) {
                Iterator<WatchEvent> iterator = response.getEvents().iterator();
                while(iterator.hasNext()){
                    WatchEvent watchEvent = iterator.next();
                    String value = watchEvent.getKeyValue().getValue().toString(StandardCharsets.UTF_8);
                    switch(watchEvent.getEventType()){
                        case PUT: {
                            ServiceInstance instance = GsonUtils.fromJson(
                                    watchEvent.getKeyValue().getValue().toString(StandardCharsets.UTF_8), ServiceInstance.class);
                            listener.notify(Collections.singletonList(instance),
                                    Collections.emptyList());
                        }
                            break;
                        case DELETE: {
                            //value does not exist in the delete event.
                            String keyArray[] = watchEvent.getKeyValue().getKey().toString(StandardCharsets.UTF_8).split("_");
                            String ipPorts = keyArray[keyArray.length-1];
                            ServiceInstance instance = new ServiceInstance(ipPorts);
                            listener.notify(Collections.emptyList(),
                                    Collections.singletonList(instance));
                        }
                            break;
                        default:
                            break;
                    }
                }
            }
            @Override
            public void onError(Throwable throwable) {
                log.warn("subscribe error, ex:",throwable);
            }
            @Override
            public void onCompleted() {
                log.info("subscribe completed.");
            }
        });
        watchMap.putIfAbsent(subscribeInfo,watchClient);
    }

    @Override
    public void doUnsubscribe(SubscribeInfo subscribeInfo) throws Exception {
        Watch watchClient = watchMap.remove(subscribeInfo);
        if(watchClient != null){
            watchClient.close();
        }
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
        leaseMap.putIfAbsent(registerKey,putOption.getLeaseId());
        kvClient.put(registerKey,registerValue,putOption);
        leaseClient.keepAlive(putOption.getLeaseId(), new StreamObserver<LeaseKeepAliveResponse>() {
            @Override
            public void onNext(LeaseKeepAliveResponse leaseKeepAliveResponse) {

            }
            @Override
            public void onError(Throwable throwable) {
                log.warn("register error, ex:",throwable);
            }
            @Override
            public void onCompleted() {
                log.info("register completed.");
            }
        });
    }

    @Override
    public void doUnregister(RegisterInfo registerInfo){
        try{
            ByteSequence registerKey = ByteSequence.from(
                    getRegisterKey(registerInfo).getBytes(StandardCharsets.UTF_8));
            Long leaseId = leaseMap.remove(registerKey);
            if(leaseId == null){
                return;
            }
            client.getLeaseClient().revoke(leaseId);
            client.getKVClient().delete(registerKey);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private String getRegisterKey(RegisterInfo registerInfo) {
        StringBuilder registerKey = new StringBuilder();
        registerKey.append(registerInfo.getGroup()).append("_");
        registerKey.append(registerInfo.getInterfaceName()).append("_");
        registerKey.append(registerInfo.getVersion()).append("_");
        registerKey.append(registerInfo.getHost()).append(":");
        registerKey.append(registerInfo.getPort());
        return registerKey.toString();
    }

    private String getSubscribeKey(SubscribeInfo subscribeInfo) {
        StringBuilder subscribeKey = new StringBuilder();
        subscribeKey.append(subscribeInfo.getGroup()).append("_");
        subscribeKey.append(subscribeInfo.getInterfaceName()).append("_");
        subscribeKey.append(subscribeInfo.getVersion());
        return subscribeKey.toString();
    }

    private String getRegisterValue(RegisterInfo registerInfo) {
        Endpoint endPoint = new Endpoint(registerInfo.getHost(), registerInfo.getPort());
        return GsonUtils.toJson(endPoint);
    }
}
