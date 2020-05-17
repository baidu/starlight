package com.baidu.brpc.naming;

import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.protocol.SubscribeInfo;
import com.baidu.brpc.utils.CustomThreadFactory;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import io.netty.util.internal.ConcurrentSet;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
public abstract class FailbackNamingService implements NamingService {


    protected ConcurrentSet<RegisterInfo> failedRegisters =
            new ConcurrentSet<RegisterInfo>();
    protected ConcurrentSet<RegisterInfo> failedUnregisters =
            new ConcurrentSet<RegisterInfo>();
    protected ConcurrentMap<SubscribeInfo, NotifyListener> failedSubscribes =
            new ConcurrentHashMap<SubscribeInfo, NotifyListener>();
    protected ConcurrentSet<SubscribeInfo> failedUnsubscribes =
            new ConcurrentSet<SubscribeInfo>();

    private Timer timer;

    private int retryInterval;

    private BrpcURL url;

    public FailbackNamingService(BrpcURL url) {
        this.url = url;
        this.retryInterval = url.getIntParameter(Constants.INTERVAL, Constants.DEFAULT_INTERVAL);
        String namingServiceClassName = this.getClass().getSimpleName();
        timer = new HashedWheelTimer(new CustomThreadFactory(namingServiceClassName + "-retry-timer-thread"));
        timer.newTimeout(
                new TimerTask() {
                    @Override
                    public void run(Timeout timeout) throws Exception {
                        try {
                            for (RegisterInfo registerInfo : failedRegisters) {
                                register(registerInfo);
                            }
                            for (RegisterInfo registerInfo : failedUnregisters) {
                                unregister(registerInfo);
                            }
                            for (Map.Entry<SubscribeInfo, NotifyListener> entry : failedSubscribes.entrySet()) {
                                subscribe(entry.getKey(), entry.getValue());
                            }
                            for (SubscribeInfo subscribeInfo : failedUnsubscribes) {
                                unsubscribe(subscribeInfo);
                            }
                        } catch (Exception ex) {
                            log.warn("retry timer exception:", ex);
                        }
                        timer.newTimeout(this, retryInterval, TimeUnit.MILLISECONDS);
                    }
                },
                retryInterval, TimeUnit.MILLISECONDS);
    }

    @Override
    public void subscribe(SubscribeInfo subscribeInfo, NotifyListener listener) {
        try {
            doSubscribe(subscribeInfo, listener);
            failedSubscribes.remove(subscribeInfo);
        } catch (Exception ex) {
            if (!subscribeInfo.isIgnoreFailOfNamingService()) {
                throw new RpcException("subscribe failed from " + url, ex);
            } else {
                failedSubscribes.putIfAbsent(subscribeInfo, listener);
            }
        }
    }

    @Override
    public void unsubscribe(SubscribeInfo subscribeInfo) {
        try {
            doUnsubscribe(subscribeInfo);
            failedUnsubscribes.remove(subscribeInfo);
        } catch (Exception ex) {
            if (!subscribeInfo.isIgnoreFailOfNamingService()) {
                throw new RpcException("unsubscribe failed from " + url, ex);
            } else {
                failedUnsubscribes.add(subscribeInfo);
            }
        }
    }

    @Override
    public void register(RegisterInfo registerInfo) {
        try {
            doRegister(registerInfo);
            failedRegisters.remove(registerInfo);
        } catch (Exception ex) {
            if (!registerInfo.isIgnoreFailOfNamingService()) {
                throw new RpcException("Failed to register to " + url, ex);
            } else {
                failedRegisters.add(registerInfo);
            }
        }
    }

    @Override
    public void unregister(RegisterInfo registerInfo) {
        try {
            doUnregister(registerInfo);
            failedUnregisters.remove(registerInfo);
        } catch (Exception ex) {
            if (!registerInfo.isIgnoreFailOfNamingService()) {
                throw new RpcException("Failed to unregister from " + url, ex);
            } else {
                failedUnregisters.add(registerInfo);
            }
        }
    }

    @Override
    public void destroy() {
        timer.stop();
    }

    public abstract void doSubscribe(SubscribeInfo subscribeInfo, NotifyListener listener) throws Exception;

    public abstract void doUnsubscribe(SubscribeInfo subscribeInfo) throws Exception;

    public abstract void doRegister(RegisterInfo registerInfo) throws Exception;

    public abstract void doUnregister(RegisterInfo registerInfo) throws Exception;



}
