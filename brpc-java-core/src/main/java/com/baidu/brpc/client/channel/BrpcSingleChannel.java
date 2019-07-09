package com.baidu.brpc.client.channel;

import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.baidu.brpc.ChannelInfo;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.client.instance.ServiceInstance;
import com.baidu.brpc.utils.CustomThreadFactory;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

/**
 * BrpcSingleChannel class keeps single persistent connection with one server
 */
@Slf4j
public class BrpcSingleChannel extends AbstractBrpcChannel {

    private static final int RETRY_THRESHOLD = 2;

    private volatile Channel channel;

    private volatile Long lastTryConnectTime = 0L;
    private AtomicInteger retryCount = new AtomicInteger(0);
    private int connectPeriod;

    private AtomicLong failedNum = new AtomicLong(0);
    private int readTimeOut;
    private int latencyWindowSize;
    private Queue<Integer> latencyWindow;


    private static final ExecutorService CONNECTION_SERVICE = Executors.newFixedThreadPool(3, new CustomThreadFactory(
            "single-channel-connect-thread"));

    public static class ReConnectTask implements Runnable {
        BrpcSingleChannel channelGroup;
        Channel oldChannel;

        public ReConnectTask(BrpcSingleChannel singleChannelGroup, Channel oldChannel) {
            this.channelGroup = singleChannelGroup;
            this.oldChannel = oldChannel;
        }

        @Override
        public void run() {
            if (oldChannel != channelGroup.channel) {
                return;
            }
            // avoid busy connecting
            if (System.currentTimeMillis() - channelGroup.lastTryConnectTime < channelGroup.connectPeriod
                    && channelGroup.retryCount.get() >= RETRY_THRESHOLD) {
                return;
            }
            synchronized (channelGroup) {
                if (oldChannel != channelGroup.channel) {
                    return;
                }
                Channel newChannel = null;
                try {
                    newChannel = channelGroup.createChannel(
                            channelGroup.getServiceInstance().getIp(),
                            channelGroup.getServiceInstance().getPort());
                } catch (Exception e) {
                    log.info("failed reconnecting");
                }
                if (newChannel != null) {
                    channelGroup.updateChannel(newChannel);
                    if (oldChannel != null) {
                        oldChannel.close();
                    }
                }
            }
        }
    }

    public BrpcSingleChannel(ServiceInstance serviceInstance, RpcClient rpcClient) {
        super(serviceInstance, rpcClient.getBootstrap(), rpcClient.getProtocol(), rpcClient);
        RpcClientOptions rpcClientOptions = rpcClient.getRpcClientOptions();
        this.connectPeriod = rpcClientOptions.getHealthyCheckIntervalMillis();
        this.readTimeOut = rpcClientOptions.getReadTimeoutMillis();
        this.latencyWindowSize = rpcClientOptions.getLatencyWindowSizeOfFairLoadBalance();
        this.latencyWindow = new ConcurrentLinkedQueue<Integer>();
    }

    @Override
    public Channel getChannel() throws Exception, NoSuchElementException, IllegalStateException {
        if (isNonActive(channel)) {
            synchronized (this) {
                if (isNonActive(channel)) {
                    channel = createChannel(serviceInstance.getIp(), serviceInstance.getPort());
                }
            }
        }
        return channel;
    }

    @Override
    public void removeChannel(Channel channel) {
        if (channel != this.channel) {
            return;
        }
        CONNECTION_SERVICE.execute(genReconnectTask(channel));
    }

    @Override
    public void updateChannel(Channel channel) {
        if (channel != this.channel) {
            this.channel = channel;
        }
    }

    private ReConnectTask genReconnectTask(Channel oldChannel) {
        return new ReConnectTask(this, oldChannel);
    }

    private Channel createChannel(String ip, int port) {
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - lastTryConnectTime < connectPeriod
                && retryCount.getAndIncrement() >= RETRY_THRESHOLD) {
            return null;
        } else {
            if (currentTimeMillis - lastTryConnectTime >= connectPeriod) {
                refreshConnectionState(currentTimeMillis, 1);
            }
            Channel channel;
            channel = doCreateChannel(ip, port);
            refreshConnectionState(currentTimeMillis, 0);
            return channel;
        }
    }

    private void refreshConnectionState(long currentTimeMillis, int retryCount) {
        this.retryCount = new AtomicInteger(retryCount);
        lastTryConnectTime = currentTimeMillis;
    }

    private Channel doCreateChannel(String ip, int port) {
        Channel channel = connect(ip, port);
        ChannelInfo channelInfo = ChannelInfo.getOrCreateClientChannelInfo(channel);
        channelInfo.setProtocol(protocol);
        channelInfo.setChannelGroup(this);
        return channel;
    }

    @Override
    public void close() {
        if (channel != null) {
            channel.close();
            channel = null;
        }
    }

    @Override
    public int getCurrentMaxConnection() {
        return countChannel();
    }

    @Override
    public int getActiveConnectionNum() {
        return countChannel();
    }

    @Override
    public int getIdleConnectionNum() {
        return countChannel();
    }

    @Override
    public void returnChannel(Channel channel) {
        // ignore
    }

    @Override
    public void updateMaxConnection(int num) {
        // ignore
    }

    private boolean isActive(Channel channel) {
        return channel != null && channel.isActive();
    }

    private boolean isNonActive(Channel channel) {
        return !isActive(channel);
    }

    private int countChannel() {
        return isActive(channel) ? 1 : 0;
    }

    @Override
    public long getFailedNum() {
        return failedNum.get();
    }

    @Override
    public void incFailedNum() {
        failedNum.incrementAndGet();
    }

    @Override
    public Queue<Integer> getLatencyWindow() {
        return latencyWindow;
    }

    @Override
    public void updateLatency(int latency) {
        latencyWindow.add(latency);
        if (latencyWindow.size() > latencyWindowSize) {
            latencyWindow.poll();
        }
    }

    @Override
    public void updateLatencyWithReadTimeOut() {
        updateLatency(readTimeOut);
    }
}
