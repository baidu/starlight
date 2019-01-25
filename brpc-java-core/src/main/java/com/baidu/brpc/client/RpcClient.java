/*
 * Copyright (c) 2018 Baidu, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baidu.brpc.client;

import com.baidu.brpc.ChannelInfo;
import com.baidu.brpc.client.endpoint.EndPoint;
import com.baidu.brpc.client.handler.RpcClientHandler;
import com.baidu.brpc.client.loadbalance.FairStrategy;
import com.baidu.brpc.client.loadbalance.LoadBalanceStrategy;
import com.baidu.brpc.client.loadbalance.LoadBalanceType;
import com.baidu.brpc.client.loadbalance.RandomStrategy;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.naming.BrpcURL;
import com.baidu.brpc.naming.DefaultNamingServiceFactory;
import com.baidu.brpc.naming.DnsNamingService;
import com.baidu.brpc.naming.NamingOptions;
import com.baidu.brpc.naming.NamingService;
import com.baidu.brpc.naming.NamingServiceFactory;
import com.baidu.brpc.naming.NotifyListener;
import com.baidu.brpc.naming.SubscribeInfo;
import com.baidu.brpc.protocol.Protocol;
import com.baidu.brpc.protocol.ProtocolManager;
import com.baidu.brpc.protocol.RpcContext;
import com.baidu.brpc.protocol.RpcRequest;
import com.baidu.brpc.protocol.RpcResponse;
import com.baidu.brpc.utils.CustomThreadFactory;
import com.baidu.brpc.utils.ThreadPool;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.Socket;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Created by huwenwei on 2017/4/25.
 */
@SuppressWarnings("unchecked")
public class RpcClient {
    private static final Logger LOG = LoggerFactory.getLogger(RpcClient.class);

    private RpcClientOptions rpcClientOptions = new RpcClientOptions();
    private Bootstrap bootstrap;
    private Timer timeoutTimer;
    private Protocol protocol;
    private CopyOnWriteArrayList<EndPoint> endPoints;
    private CopyOnWriteArrayList<BrpcChannelGroup> healthyInstances;
    private CopyOnWriteArrayList<BrpcChannelGroup> unhealthyInstances;
    private Timer healthCheckTimer;
    private LoadBalanceStrategy loadBalanceStrategy;
    private List<Interceptor> interceptors = new ArrayList<Interceptor>();
    private NamingService namingService;
    private ThreadPool threadPool;
    private Class serviceInterface;
    private SubscribeInfo subscribeInfo;
    private AtomicBoolean isStop = new AtomicBoolean(false);

    /**
     * 出错时触发用户回调方法的线程
     */
    private ExecutorService callbackThread = Executors.newScheduledThreadPool(1,
            new CustomThreadFactory("invalid-channel-callback-thread"));

    /**
     * 保存单例的引用
     */
    private FastFutureStore fastFutureStore;

    public RpcClient(String namingServiceUrl) {
        this(namingServiceUrl, new RpcClientOptions(), null, null);
    }

    public RpcClient(String namingServiceUrl, RpcClientOptions options) {
        this(namingServiceUrl, options, null, null);
    }

    public RpcClient(String namingServiceUrl, RpcClientOptions options, List<Interceptor> interceptors) {
        this(namingServiceUrl, options, interceptors, null);
    }

    /**
     * parse naming service url, connect to servers
     *
     * @param serviceUrl format like "list://127.0.0.1:8200"
     * @param options          rpc client options
     */
    public RpcClient(String serviceUrl,
                     final RpcClientOptions options,
                     List<Interceptor> interceptors,
                     NamingServiceFactory namingServiceFactory) {
        Validate.notEmpty(serviceUrl);
        Validate.notNull(options);
        this.init(options, interceptors);
        // parse naming
        BrpcURL url = new BrpcURL(serviceUrl);
        if (namingServiceFactory != null) {
            this.namingService = namingServiceFactory.createNamingService(url);
        } else {
            this.namingService = new DefaultNamingServiceFactory().createNamingService(url);
        }
    }

    public RpcClient(EndPoint endPoint) {
        this(endPoint, new RpcClientOptions());
    }

    public RpcClient(EndPoint endPoint, RpcClientOptions options) {
        this.init(options, null);
        List<EndPoint> endPoints = new ArrayList<EndPoint>(1);
        endPoints.add(endPoint);
        this.addEndPoints(endPoints);
    }

    public RpcClient(List<EndPoint> endPoints) {
        this(endPoints, new RpcClientOptions(), null);
    }

    public RpcClient(List<EndPoint> endPoints, RpcClientOptions option, List<Interceptor> interceptors) {
        this.init(option, interceptors);
        this.addEndPoints(endPoints);
    }

    public void setServiceInterface(Class clazz) {
        setServiceInterface(clazz, null);
    }

    public void setServiceInterface(Class clazz, NamingOptions namingOptions) {
        if (this.serviceInterface != null) {
            throw new RpcException("serviceInterface must not be set repeatedly, please use another RpcClient");
        }
        if (clazz.getInterfaces().length == 0) {
            this.serviceInterface = clazz;
        } else {
            // if it is async interface, we should subscribe the sync interface
            this.serviceInterface = clazz.getInterfaces()[0];
        }

        if (namingService != null) {
            subscribeInfo = new SubscribeInfo();
            subscribeInfo.setService(serviceInterface.getName());
            if (namingOptions != null) {
                subscribeInfo.setGroup(namingOptions.getGroup());
                subscribeInfo.setVersion(namingOptions.getVersion());
                subscribeInfo.setIgnoreFailOfNamingService(namingOptions.isIgnoreFailOfNamingService());
            }
            List<EndPoint> endPoints = this.namingService.lookup(subscribeInfo);
            addEndPoints(endPoints);
            this.namingService.subscribe(subscribeInfo, new NotifyListener() {
                @Override
                public void notify(Collection<EndPoint> addList, Collection<EndPoint> deleteList) {
                    addEndPoints(addList);
                    deleteEndPoints(deleteList);
                }
            });
        }
    }

    public void stop() {
        // avoid stop multi times
        if (isStop.compareAndSet(false, true)) {
            if (bootstrap.config().group() != null) {
                Future future = bootstrap.config().group().shutdownGracefully();
                ((io.netty.util.concurrent.Future) future).syncUninterruptibly();
            }
            for (BrpcChannelGroup channelGroup : healthyInstances) {
                channelGroup.close();
            }
            for (BrpcChannelGroup channelGroup : unhealthyInstances) {
                channelGroup.close();
            }
            if (timeoutTimer != null) {
                timeoutTimer.stop();
            }
            if (namingService != null) {
                namingService.unsubscribe(subscribeInfo);
            }
            if (healthCheckTimer != null) {
                healthCheckTimer.stop();
            }
            if (loadBalanceStrategy != null) {
                loadBalanceStrategy.destroy();
            }
            if (callbackThread != null) {
                callbackThread.shutdown();
            }
            if (threadPool != null) {
                threadPool.stop();
            }
        }
    }

    /**
     * 业务手动选择channel，
     * 这类channel由业务自己调用returnChannel归还给连接池，
     * 或者调用removeChannel从连接池中删除。
     * @return netty channel
     */
    public Channel selectChannel() {
        boolean isHealthInstance = true;
        BrpcChannelGroup channelGroup = loadBalanceStrategy.selectInstance(healthyInstances);
        if (channelGroup == null) {
            LOG.debug("no available healthy server, so random select one unhealthy server");
            RandomStrategy randomStrategy = new RandomStrategy();
            randomStrategy.init(this);
            channelGroup = randomStrategy.selectInstance(unhealthyInstances);
            if (channelGroup == null) {
                throw new RpcException(RpcException.NETWORK_EXCEPTION, "no available instance");
            }
            isHealthInstance = false;
        }
        Channel channel;
        try {
            channel = channelGroup.getChannel();
        } catch (NoSuchElementException full) {
            int maxConnections = channelGroup.getChannelFuturePool().getMaxTotal() * 2;
            channelGroup.getChannelFuturePool().setMaxTotal(maxConnections);
            channelGroup.getChannelFuturePool().setMaxIdle(maxConnections);
            String errMsg = String.format("channel pool is exhausted, and double maxTotalConnection,server=%s:%d",
                    channelGroup.getIp(), channelGroup.getPort());
            LOG.debug(errMsg);
            throw new RpcException(RpcException.NETWORK_EXCEPTION, errMsg);
        } catch (IllegalStateException illegalState) {
            String errMsg = String.format("channel pool is closed, server=%s:%d",
                    channelGroup.getIp(), channelGroup.getPort());
            LOG.debug(errMsg);
            throw new RpcException(RpcException.UNKNOWN_EXCEPTION, errMsg);
        } catch (Exception connectedFailed) {
            String errMsg = String.format("channel pool make new object failed, "
                            + "active=%d,idle=%d,server=%s:%d, ex=%s",
                    channelGroup.getChannelFuturePool().getNumActive(),
                    channelGroup.getChannelFuturePool().getNumIdle(),
                    channelGroup.getIp(), channelGroup.getPort(), connectedFailed.getMessage());
            LOG.debug(errMsg);
            if (isHealthInstance) {
                healthyInstances.remove(channelGroup);
                notifyInvalidInstance(Collections.singletonList(channelGroup));
                unhealthyInstances.add(channelGroup);
            }
            throw new RpcException(RpcException.UNKNOWN_EXCEPTION, errMsg);
        }

        if (channel == null) {
            String errMsg = "channel is null, retry another channel";
            LOG.debug(errMsg);
            throw new RpcException(RpcException.UNKNOWN_EXCEPTION, errMsg);
        }
        if (!channel.isActive()) {
            channelGroup.incFailedNum();
            // 如果连接不是有效的，从连接池中剔除。
            channelGroup.removeChannel(channel);
            String errMsg = "channel is non active, retry another channel";
            throw new RpcException(RpcException.NETWORK_EXCEPTION, errMsg);
        }
        return channel;
    }

    public void returnChannel(Channel channel) {
        ChannelInfo channelInfo = ChannelInfo.getClientChannelInfo(channel);
        channelInfo.getChannelGroup().returnChannel(channel);
    }

    public void removeChannel(Channel channel) {
        ChannelInfo channelInfo = ChannelInfo.getClientChannelInfo(channel);
        channelInfo.getChannelGroup().removeChannel(channel);
    }

    public <T> Future<T> sendRequest(
            final RpcRequest rpcRequest,
            Type responseType, final RpcCallback<T> callback, RpcFuture rpcFuture, final boolean isFinalTry) {
        // 如果业务在RpcContext中设置了channel，则不再通过负载均衡选择channel
        final RpcContext rpcContext = RpcContext.getContext();
        Channel channel = rpcContext.getChannel();
        if (channel == null) {
            channel = selectChannel();
        }
        LOG.debug("channel={}", channel);
        final ChannelInfo channelInfo = ChannelInfo.getClientChannelInfo(channel);
        BrpcChannelGroup channelGroup = channelInfo.getChannelGroup();

        if (rpcClientOptions.isHttp() && !rpcRequest.headers().contains(HttpHeaderNames.HOST)) {
            String hostPort;
            if (namingService != null && namingService instanceof DnsNamingService) {
                // 从 DnsNamingService 获取原始的 host
                hostPort = ((DnsNamingService) namingService).getHostPort();
            } else {
                // 默认获取当前链接的 host:port 即可
                hostPort = channelGroup.getIp() + ":" + channelGroup.getPort();
            }
            rpcRequest.headers().set(HttpHeaderNames.HOST, hostPort);
        }

        // add request to RpcFuture and add timeout task
        final long readTimeout = getRpcClientOptions().getReadTimeoutMillis();
        Timeout timeout = timeoutTimer.newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                boolean isSyncAndFinalRequest = isSyncAndFinalRequest();

                RpcFuture rpcFuture;
                if (isSyncAndFinalRequest) {
                    // get and remove
                    rpcFuture = channelInfo.removeRpcFuture(rpcRequest.getLogId());

                } else {
                    // get only
                    rpcFuture = channelInfo.getRpcFuture(rpcRequest.getLogId());
                }

                if (rpcFuture != null) {
                    String ip = rpcFuture.getChannelInfo().getChannelGroup().getIp();
                    int port = rpcFuture.getChannelInfo().getChannelGroup().getPort();
                    long elapseTime = System.currentTimeMillis() - rpcFuture.getStartTime();
                    String errMsg = String.format("request timeout,logId=%d,ip=%s,port=%d,elapse=%dms",
                            rpcRequest.getLogId(), ip, port, elapseTime);
                    LOG.info(errMsg);
                    RpcResponse rpcResponse = new RpcResponse();
                    rpcResponse.setException(new RpcException(RpcException.TIMEOUT_EXCEPTION, errMsg));

                    if (isSyncAndFinalRequest) {
                        rpcFuture.handleResponse(rpcResponse);

                    } else {
                        rpcFuture.processConnection(rpcResponse);
                    }
                }
            }

            private boolean isSyncAndFinalRequest() {
                return callback != null || rpcContext.getChannel() != null || isFinalTry;
            }

        }, readTimeout, TimeUnit.MILLISECONDS);

        try {
            // set the missing parameters
            rpcFuture.setTimeout(timeout);
            rpcFuture.setChannelInfo(channelInfo);
            rpcFuture.setRpcClient(this);

            channelInfo.setLogId(rpcFuture.getLogId());

            if (rpcClientOptions.isHttp()) {
                try {
                    protocol.encodeHttpRequest(rpcRequest);
                } catch (Exception ex) {
                    throw new RpcException(ex);
                }
            }

        } catch (RpcException ex) {
            timeout.cancel();
            if (!channelInfo.isFromRpcContext()) {
                channelInfo.getChannelGroup().returnChannel(channel);
            }
            throw ex;
        }

        try {
            // netty在发送完请求后会release，
            // 所以这里要先retain，防止在重试时，refCnt变成0
            rpcRequest.addRefCnt();
            ChannelFuture sendFuture = null;
            if (!rpcClientOptions.isHttp()) {
                ByteBuf requestBuf = channelInfo.getProtocol().encodeRequest(rpcRequest);
                // netty在调用完writeAndFlush后，会release requestBuf
                sendFuture = channel.writeAndFlush(requestBuf);
            } else {
                sendFuture = channel.writeAndFlush(rpcRequest);
            }
            sendFuture.awaitUninterruptibly(rpcClientOptions.getWriteTimeoutMillis());
            if (!sendFuture.isSuccess()) {
                channelInfo.handleRequestFail();
                healthyInstances.remove(channelGroup);
                notifyInvalidInstance(Collections.singletonList(channelGroup));
                if (!unhealthyInstances.contains(channelGroup)) {
                    unhealthyInstances.add(channelGroup);
                }
                timeout.cancel();
                if (!channelInfo.isFromRpcContext()) {
                    channelInfo.getChannelGroup().returnChannel(channel);
                }
                if (!(sendFuture.cause() instanceof ClosedChannelException)) {
                    LOG.warn("send request failed, channelActive={}, ex=",
                            channel.isActive(), sendFuture.cause());
                }
                String errMsg = String.format("send request failed, channelActive=%b, ex=%s",
                        channel.isActive(), sendFuture.cause().getMessage());
                throw new RpcException(RpcException.NETWORK_EXCEPTION, errMsg);
            }
        } catch (Exception ex) {
            throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, ex.getMessage());
        }

        // return channel
        channelInfo.handleRequestSuccess();

        return rpcFuture;
    }

    public void triggerCallback(Runnable runnable) {
        callbackThread.execute(runnable);
    }

    private void init(final RpcClientOptions options, List<Interceptor> interceptors) {
        Validate.notNull(options);
        try {
            BeanUtils.copyProperties(this.rpcClientOptions, options);
        } catch (Exception ex) {
            LOG.warn("init rpc options failed, so use default");
        }
        if (interceptors != null) {
            this.interceptors = interceptors;
        }
        this.protocol = ProtocolManager.instance().init(options.getEncoding()).getProtocol(options.getProtocolType());
        fastFutureStore = FastFutureStore.getInstance(options.getFutureBufferSize());
        timeoutTimer = new HashedWheelTimer(new CustomThreadFactory("timeout-timer-thread"));

        // 负载均衡算法
        LoadBalanceType loadBalanceType = LoadBalanceType.parse(rpcClientOptions.getLoadBalanceType());
        loadBalanceStrategy = loadBalanceType.getStrategy();
        loadBalanceStrategy.init(this);

        this.endPoints = new CopyOnWriteArrayList<EndPoint>();
        this.healthyInstances = new CopyOnWriteArrayList<BrpcChannelGroup>();
        this.unhealthyInstances = new CopyOnWriteArrayList<BrpcChannelGroup>();
        healthCheckTimer = new HashedWheelTimer(new CustomThreadFactory("health-check-timer-thread"));
        this.threadPool = new ThreadPool(rpcClientOptions.getWorkThreadNum(),
                new CustomThreadFactory("client-work-thread"));

        // init netty bootstrap
        bootstrap = new Bootstrap();
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, rpcClientOptions.getConnectTimeoutMillis());
        bootstrap.option(ChannelOption.SO_KEEPALIVE, rpcClientOptions.isKeepAlive());
        bootstrap.option(ChannelOption.SO_REUSEADDR, rpcClientOptions.isReuseAddr());
        bootstrap.option(ChannelOption.TCP_NODELAY, rpcClientOptions.isTcpNoDelay());
        bootstrap.option(ChannelOption.SO_RCVBUF, rpcClientOptions.getReceiveBufferSize());
        bootstrap.option(ChannelOption.SO_SNDBUF, rpcClientOptions.getSendBufferSize());
        final RpcClientHandler rpcClientHandler = new RpcClientHandler(RpcClient.this);
        final ChannelInitializer<SocketChannel> initializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                if (rpcClientOptions.isHttp()) {
                    ch.pipeline().addLast(new HttpClientCodec());
                    ch.pipeline().addLast(new HttpObjectAggregator(10 * 1024 * 1024));
                }
                ch.pipeline().addLast(rpcClientHandler);
            }
        };
        bootstrap.group(new NioEventLoopGroup(
                options.getIoThreadNum(),
                new CustomThreadFactory("client-io-thread")))
                .handler(initializer);

        // 开启健康检查线程
        healthCheckTimer.newTimeout(
                new TimerTask() {
                    @Override
                    public void run(Timeout timeout) throws Exception {
                        List<BrpcChannelGroup> newHealthyInstances = new ArrayList<BrpcChannelGroup>();
                        Iterator<BrpcChannelGroup> iter = unhealthyInstances.iterator();
                        while (iter.hasNext()) {
                            BrpcChannelGroup instance = iter.next();
                            boolean isHealthy = isInstanceHealthy(instance.getIp(), instance.getPort());
                            if (isHealthy) {
                                newHealthyInstances.add(instance);
                            }
                        }

                        List<BrpcChannelGroup> newUnhealthyInstances = new ArrayList<BrpcChannelGroup>();
                        iter = healthyInstances.iterator();
                        while (iter.hasNext()) {
                            BrpcChannelGroup instance = iter.next();
                            boolean isHealthy = isInstanceHealthy(instance.getIp(), instance.getPort());
                            if (!isHealthy) {
                                newUnhealthyInstances.add(instance);
                            }
                        }

                        healthyInstances.addAll(newHealthyInstances);
                        unhealthyInstances.removeAll(newHealthyInstances);

                        healthyInstances.removeAll(newUnhealthyInstances);
                        unhealthyInstances.addAll(newUnhealthyInstances);
                        notifyInvalidInstance(newUnhealthyInstances);

                        healthCheckTimer.newTimeout(this, options.getHealthyCheckIntervalMillis(),
                                TimeUnit.MILLISECONDS);
                    }
                },
                options.getHealthyCheckIntervalMillis(),
                TimeUnit.MILLISECONDS);
    }

    private boolean isInstanceHealthy(String ip, int port) {
        boolean isHealthy = false;
        Socket socket = null;
        try {
            socket = new Socket(ip, port);
            isHealthy = true;
        } catch (Exception e) {
            LOG.warn("Recover socket test for {}:{} failed. message:{}",
                    ip, port, e.getMessage());
            isHealthy = false;
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(e.getMessage(), e);
                }
            }
        }
        return isHealthy;
    }

    public void updateEndPoints(List<EndPoint> newEndPoints) {
        Collection<EndPoint> addList = CollectionUtils.subtract(newEndPoints, endPoints);
        Collection<EndPoint> deleteList = CollectionUtils.subtract(endPoints, newEndPoints);
        for (EndPoint endPoint : addList) {
            addEndPoint(endPoint);
        }

        for (EndPoint endPoint : deleteList) {
            deleteEndPoint(endPoint);
        }
    }

    public void addEndPoints(Collection<EndPoint> addList) {
        for (EndPoint endPoint : addList) {
            addEndPoint(endPoint);
        }
    }

    protected void deleteEndPoints(Collection<EndPoint> deleteList) {
        for (EndPoint endPoint : deleteList) {
            deleteEndPoint(endPoint);
        }
    }

    private void addEndPoint(EndPoint endPoint) {
        if (endPoints.contains(endPoint)) {
            LOG.warn("endpoint already exist, {}:{}", endPoint.getIp(), endPoint.getPort());
            return;
        }
        healthyInstances.add(new BrpcChannelGroup(endPoint.getIp(), endPoint.getPort(), this));
        endPoints.add(endPoint);
    }

    private void deleteEndPoint(EndPoint endPoint) {
        List<BrpcChannelGroup> removedInstances = new LinkedList<BrpcChannelGroup>();

        Iterator<BrpcChannelGroup> iterator = healthyInstances.iterator();
        while (iterator.hasNext()) {
            BrpcChannelGroup channelGroup = iterator.next();
            if (channelGroup.getIp().equals(endPoint.getIp())
                    && channelGroup.getPort() == endPoint.getPort()) {
                channelGroup.close();
                healthyInstances.remove(channelGroup);
                removedInstances.add(channelGroup);
                break;
            }
        }

        iterator = unhealthyInstances.iterator();
        while (iterator.hasNext()) {
            BrpcChannelGroup channelGroup = iterator.next();
            if (channelGroup.getIp().equals(endPoint.getIp())
                    && channelGroup.getPort() == endPoint.getPort()) {
                channelGroup.close();
                unhealthyInstances.remove(channelGroup);
                break;
            }
        }
        endPoints.remove(endPoint);

        // notify the fair load balance strategy
        notifyInvalidInstance(removedInstances);
    }

    public void removeLogId(long id) {
        fastFutureStore.getAndRemove(id);
    }

    public RpcClientOptions getRpcClientOptions() {
        return rpcClientOptions;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public CopyOnWriteArrayList<BrpcChannelGroup> getHealthyInstances() {
        return healthyInstances;
    }

    public CopyOnWriteArrayList<BrpcChannelGroup> getUnhealthyInstances() {
        return unhealthyInstances;
    }

    public List<Interceptor> getInterceptors() {
        return interceptors;
    }

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    public ThreadPool getThreadPool() {
        return threadPool;
    }

    private void notifyInvalidInstance(List<BrpcChannelGroup> invalidInstances) {
        if (rpcClientOptions.getLoadBalanceType() == LoadBalanceType.FAIR.getId()) {
            ((FairStrategy) loadBalanceStrategy).markInvalidInstance(invalidInstances);
        }
    }
}
