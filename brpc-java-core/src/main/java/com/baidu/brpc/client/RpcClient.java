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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.brpc.ChannelInfo;
import com.baidu.brpc.Controller;
import com.baidu.brpc.client.channel.BrpcChannelGroup;
import com.baidu.brpc.client.channel.ChannelType;
import com.baidu.brpc.client.endpoint.BasicEndPointProcessor;
import com.baidu.brpc.client.endpoint.EndPoint;
import com.baidu.brpc.client.endpoint.EndPointProcessor;
import com.baidu.brpc.client.endpoint.EnhancedEndPointProcessor;
import com.baidu.brpc.client.handler.IdleChannelHandler;
import com.baidu.brpc.client.handler.RpcClientHandler;
import com.baidu.brpc.client.loadbalance.LoadBalanceStrategy;
import com.baidu.brpc.client.loadbalance.LoadBalanceType;
import com.baidu.brpc.client.loadbalance.RandomStrategy;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.interceptor.JoinPoint;
import com.baidu.brpc.naming.BrpcURL;
import com.baidu.brpc.naming.DefaultNamingServiceFactory;
import com.baidu.brpc.naming.ListNamingService;
import com.baidu.brpc.naming.NamingOptions;
import com.baidu.brpc.naming.NamingService;
import com.baidu.brpc.naming.NamingServiceFactory;
import com.baidu.brpc.naming.NotifyListener;
import com.baidu.brpc.naming.SubscribeInfo;
import com.baidu.brpc.protocol.Protocol;
import com.baidu.brpc.protocol.ProtocolManager;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import com.baidu.brpc.thread.BrpcIoThreadPoolInstance;
import com.baidu.brpc.thread.BrpcWorkThreadPoolInstance;
import com.baidu.brpc.thread.ClientCallBackThreadPoolInstance;
import com.baidu.brpc.thread.ClientTimeoutTimerInstance;
import com.baidu.brpc.thread.ShutDownManager;
import com.baidu.brpc.utils.CollectionUtils;
import com.baidu.brpc.utils.ThreadPool;

import edu.emory.mathcs.backport.java.util.Collections;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollMode;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.Timer;

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
    private LoadBalanceStrategy loadBalanceStrategy;
    private List<Interceptor> interceptors = new ArrayList<Interceptor>();
    private NamingService namingService;
    private ThreadPool workThreadPool;
    private Class serviceInterface;
    private SubscribeInfo subscribeInfo;
    private AtomicBoolean isStop = new AtomicBoolean(false);
    private EndPointProcessor endPointProcessor;
    /**
     * callBack thread when method invoke fail
     */
    private ExecutorService callbackThread;

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
     * @param options    rpc client options
     */
    public RpcClient(String serviceUrl,
                     final RpcClientOptions options,
                     List<Interceptor> interceptors,
                     NamingServiceFactory namingServiceFactory) {
        Validate.notEmpty(serviceUrl);
        Validate.notNull(options);

        // parse naming
        BrpcURL url = new BrpcURL(serviceUrl);
        if (namingServiceFactory != null) {
            this.namingService = namingServiceFactory.createNamingService(url);
        } else {
            this.namingService = new DefaultNamingServiceFactory().createNamingService(url);
        }
        boolean isSingleServer = false;
        if (namingService instanceof ListNamingService) {
            List<EndPoint> endPoints = namingService.lookup(null);
            isSingleServer = endPoints.size() == 1;
        }

        this.init(options, interceptors, isSingleServer);
    }

    public RpcClient(EndPoint endPoint) {
        this(endPoint, null);
    }

    public RpcClient(EndPoint endPoint, RpcClientOptions options) {
        if (null == options) {
            options = new RpcClientOptions();
        }
        this.init(options, null, true);

        List<EndPoint> endpoints = Collections.singletonList(endPoint);

        endPointProcessor.addEndPoints(endpoints);
    }

    public RpcClient(List<EndPoint> endPoints) {
        this(endPoints, new RpcClientOptions(), null);
    }

    public RpcClient(List<EndPoint> endPoints, RpcClientOptions option, List<Interceptor> interceptors) {
        this.init(option, interceptors, endPoints.size() == 1);
        endPointProcessor.addEndPoints(endPoints);
    }

    public static <T> T getProxy(RpcClient rpcClient, Class clazz, NamingOptions namingOptions) {
        return BrpcProxy.getProxy(rpcClient, clazz, namingOptions);
    }

    public static <T> T getProxy(RpcClient rpcClient, Class clazz) {
        return BrpcProxy.getProxy(rpcClient, clazz, null);
    }

    public <T> T getProxy(Class clazz, NamingOptions namingOptions) {
        return BrpcProxy.getProxy(this, clazz, namingOptions);
    }

    public <T> T getProxy(Class clazz) {
        return BrpcProxy.getProxy(this, clazz, null);
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
            endPointProcessor.addEndPoints(endPoints);
            this.namingService.subscribe(subscribeInfo, new NotifyListener() {
                @Override
                public void notify(Collection<EndPoint> addList, Collection<EndPoint> deleteList) {
                    endPointProcessor.addEndPoints(addList);
                    endPointProcessor.deleteEndPoints(deleteList);
                }
            });
        }
    }

    public void stop() {
        // avoid stop multi times
        if (isStop.compareAndSet(false, true)) {
            if (namingService != null) {
                namingService.unsubscribe(subscribeInfo);
            }
            if (endPointProcessor != null) {
                endPointProcessor.stop();
            }
            if (loadBalanceStrategy != null) {
                loadBalanceStrategy.destroy();
            }
        }
    }

    /**
     * 业务手动选择channel，
     * 这类channel由业务自己调用returnChannel归还给连接池，
     * 或者调用removeChannel从连接池中删除。
     *
     * @return netty channel
     */
    public Channel selectChannel() {
        boolean isHealthInstance = true;
        BrpcChannelGroup channelGroup = loadBalanceStrategy.selectInstance(endPointProcessor.getHealthyInstances());
        if (channelGroup == null) {
            LOG.debug("no available healthy server, so random select one unhealthy server");
            RandomStrategy randomStrategy = new RandomStrategy();
            randomStrategy.init(this);
            channelGroup = randomStrategy.selectInstance(endPointProcessor.getUnHealthyInstances());
            if (channelGroup == null) {
                throw new RpcException(RpcException.NETWORK_EXCEPTION, "no available instance");
            }
            isHealthInstance = false;
        }
        Channel channel;
        try {
            channel = channelGroup.getChannel();
        } catch (NoSuchElementException full) {
            int maxConnections = channelGroup.getCurrentMaxConnection() * 2;
            channelGroup.updateMaxConnection(maxConnections);
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
                    channelGroup.getActiveConnectionNum(),
                    channelGroup.getIdleConnectionNum(),
                    channelGroup.getIp(), channelGroup.getPort(), connectedFailed.getMessage());
            LOG.debug(errMsg);
            if (isHealthInstance) {

                List<BrpcChannelGroup> unHealthyInstances = new ArrayList<BrpcChannelGroup>(1);
                unHealthyInstances.add(channelGroup);
                endPointProcessor.updateUnHealthyInstances(unHealthyInstances);
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

    public <T> AsyncAwareFuture<T> sendRequest(
            Controller controller, final Request request,
            RpcCallback callback) {
        // if user set channel in controller, rpc will not select channel again.
        Channel channel;
        if (controller != null && controller.getChannel() != null) {
            channel = controller.getChannel();
        } else {
            channel = selectChannel();
        }
        LOG.debug("channel={}", channel);
        final ChannelInfo channelInfo = ChannelInfo.getClientChannelInfo(channel);
        BrpcChannelGroup channelGroup = channelInfo.getChannelGroup();
        protocol.beforeRequestSent(request, this, channelGroup);

        request.setChannel(channel);

        // create RpcFuture object
        RpcFuture rpcFuture = new RpcFuture();
        rpcFuture.setRpcMethodInfo(request.getRpcMethodInfo());
        rpcFuture.setCallback(callback);
        rpcFuture.setController(controller);
        rpcFuture.setRpcClient(this);
        rpcFuture.setChannelInfo(channelInfo);
        // generate logId
        long logId = FastFutureStore.getInstance(0).put(rpcFuture);
        rpcFuture.setChannelInfo(channelInfo);

        request.setLogId(logId);

        // invoke before interceptor
        if (CollectionUtils.isNotEmpty(interceptors)) {
            for (Interceptor interceptor : interceptors) {
                boolean success = interceptor.handleRequest(request);
                if (!success) {
                    LOG.warn("interceptor return false, terminate...");
                    // clean logId before throwing exception
                    FastFutureStore.getInstance(0).getAndRemove(request.getLogId());
                    throw new RpcException(RpcException.FORBIDDEN_EXCEPTION, "interceptor");
                }
            }
        }
        // invoke around interceptor
        JoinPoint joinPoint = new ClientJoinPoint(controller, request, this, rpcFuture);
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (RpcException e) {
            throw e;
        } catch (Exception e) {
            throw new RpcException("exception thrown by around interceptor", e);
        }
        // sync: invoke after interceptor in this thread
        // async: invoke after interceptor in another thread
        if (rpcFuture.isAsync()) {
            // async: the return type must be Future, but might be replaced in around interceptor
            return (AsyncAwareFuture) result;
        } else {
            // sync: the result might be replaced in around interceptor, therefore, the result in response should be set
            Response response = rpcFuture.getResponse();
            response.setResult(result);
            if (CollectionUtils.isNotEmpty(interceptors)) {
                int length = interceptors.size();
                for (int i = length - 1; i >= 0; i--) {
                    interceptors.get(i).handleResponse(response);
                }
            }
            return rpcFuture;
        }
    }

    public void triggerCallback(Runnable runnable) {
        if (!callbackThread.isTerminated()) {
            callbackThread.execute(runnable);
        }
    }

    private void init(final RpcClientOptions options, List<Interceptor> interceptors, boolean isSingleServer) {
        Validate.notNull(options);
        try {
            this.rpcClientOptions.copyFrom(options);
        } catch (Exception ex) {
            LOG.warn("init rpc options failed, so use default");
        }
        if (interceptors != null) {
            this.interceptors = interceptors;
        }
        this.protocol = ProtocolManager.instance().init(options.getEncoding()).getProtocol(options.getProtocolType());
        fastFutureStore = FastFutureStore.getInstance(options.getFutureBufferSize());
        timeoutTimer = ClientTimeoutTimerInstance.getOrCreateInstance();

        // singleServer or isShortConnection do not need healthChecker
        if (isSingleServer || rpcClientOptions.getChannelType() == ChannelType.SHORT_CONNECTION) {
            endPointProcessor = new BasicEndPointProcessor(this);
        } else {
            endPointProcessor = new EnhancedEndPointProcessor(this);
        }

        // 负载均衡算法
        LoadBalanceType loadBalanceType = LoadBalanceType.parse(rpcClientOptions.getLoadBalanceType());
        loadBalanceStrategy = loadBalanceType.getStrategy();
        loadBalanceStrategy.init(this);

        // init once
        ShutDownManager.getInstance();

        this.workThreadPool = BrpcWorkThreadPoolInstance.getOrCreateInstance(rpcClientOptions.getWorkThreadNum());
        this.callbackThread = ClientCallBackThreadPoolInstance.getOrCreateInstance(1);

        // init netty bootstrap
        bootstrap = new Bootstrap();
        if (Epoll.isAvailable()) {
            bootstrap.channel(EpollSocketChannel.class);
            bootstrap.option(EpollChannelOption.EPOLL_MODE, EpollMode.EDGE_TRIGGERED);
        } else {
            bootstrap.channel(NioSocketChannel.class);
        }

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
                if (rpcClientOptions.getChannelType() == ChannelType.SINGLE_CONNECTION) {
                    ch.pipeline().addLast(new IdleStateHandler(0, 0, rpcClientOptions.getKeepAliveTime()));
                    ch.pipeline().addLast(new IdleChannelHandler());
                }
                ch.pipeline().addLast(rpcClientHandler);
            }
        };

        bootstrap.group(BrpcIoThreadPoolInstance.getOrCreateInstance(options.getIoThreadNum()))
                .handler(initializer);

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
        return endPointProcessor.getHealthyInstances();
    }

    public CopyOnWriteArrayList<EndPoint> getEndPoints() {
        return endPointProcessor.getEndPoints();
    }

    public List<Interceptor> getInterceptors() {
        return interceptors;
    }

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    public ThreadPool getWorkThreadPool() {
        return workThreadPool;
    }

    public LoadBalanceStrategy getLoadBalanceStrategy() {
        return loadBalanceStrategy;
    }

    public boolean isLongConnection() {
        return rpcClientOptions.getChannelType() != ChannelType.SHORT_CONNECTION;
    }

    public NamingService getNamingService() {
        return namingService;
    }

    public Timer getTimeoutTimer() {
        return timeoutTimer;
    }

    public EndPointProcessor getEndPointProcessor() {
        return endPointProcessor;
    }
}
