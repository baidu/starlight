/*
 * Copyright (c) 2019 Baidu, Inc. All Rights Reserved.
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

package com.baidu.brpc.server;

import com.baidu.brpc.ChannelInfo;
import com.baidu.brpc.CommunicationSpiManager;
import com.baidu.brpc.client.AsyncAwareFuture;
import com.baidu.brpc.client.RpcFuture;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.interceptor.ServerInvokeInterceptor;
import com.baidu.brpc.protocol.*;
import com.baidu.brpc.protocol.push.ServerPushProtocol;
import com.baidu.brpc.server.handler.RpcServerChannelIdleHandler;
import com.baidu.brpc.server.handler.RpcServerHandler;
import com.baidu.brpc.server.push.RegisterServiceImpl;
import com.baidu.brpc.thread.*;
import com.baidu.brpc.utils.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollMode;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by wenweihu86 on 2019/12/07.
 */
@Getter
@Slf4j
public class CommunicationServer {
    protected RpcServerOptions rpcServerOptions = new RpcServerOptions();

    /**
     * host to bind
     */
    protected String host;

    /**
     * port to bind
     */
    protected int port;

    /**
     * netty bootstrap
     */
    protected ServerBootstrap bootstrap;

    /**
     * netty io thread pool
     */
    protected EventLoopGroup bossGroup;
    // netty io thread pool
    protected EventLoopGroup workerGroup;
    protected Protocol protocol;
    protected ThreadPool threadPool;
    protected List<ThreadPool> customThreadPools = new ArrayList<ThreadPool>();
    protected List<Object> serviceList = new ArrayList<Object>();
    protected ServerStatus serverStatus;
    protected AtomicBoolean stop = new AtomicBoolean(false);
    protected Timer timeoutTimer;
    protected ServiceManager serviceManager = ServiceManager.getInstance();

    public CommunicationServer(int port) {
        this(null, port, new RpcServerOptions());
    }

    public CommunicationServer(String host, int port) {
        this(host, port, new RpcServerOptions());
    }

    public CommunicationServer(int port, RpcServerOptions options) {
        this(null, port, options);
    }

    public CommunicationServer(String host, int port,
                               final RpcServerOptions options) {
        this.host = host;
        this.port = port;
        if (options != null) {
            try {
                this.rpcServerOptions.copyFrom(options);
            } catch (Exception ex) {
                log.warn("init options failed, so use default");
            }
        }
        // init global
        CommunicationSpiManager.getInstance().loadAllExtensions(rpcServerOptions.getEncoding());
        // register shutdown hook to jvm
        ShutDownManager.getInstance();
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                CommunicationServer.this.shutdown();
            }
        }));

        // find protocol
        if (rpcServerOptions.getProtocolType() != null) {
            this.protocol = ProtocolManager.getInstance().getProtocol(rpcServerOptions.getProtocolType());
        }
        bootstrap = new ServerBootstrap();
        if (rpcServerOptions.isGlobalThreadPoolSharing()) {
            threadPool = ServerWorkThreadPoolInstance.getOrCreateInstance(rpcServerOptions.getWorkThreadNum());
        } else {
            threadPool = new ThreadPool(rpcServerOptions.getWorkThreadNum(),
                    new CustomThreadFactory("server-work-thread"));
        }

        if (rpcServerOptions.getIoEventType() == BrpcConstants.IO_EVENT_NETTY_EPOLL) {
            if (rpcServerOptions.isGlobalThreadPoolSharing()) {
                bossGroup = ServerAcceptorThreadPoolInstance.getOrCreateEpollInstance(rpcServerOptions.getAcceptorThreadNum());
                workerGroup = ServerIoThreadPoolInstance.getOrCreateEpollInstance(rpcServerOptions.getAcceptorThreadNum());
            } else {
                bossGroup = new EpollEventLoopGroup(rpcServerOptions.getAcceptorThreadNum(),
                        new CustomThreadFactory("server-acceptor-thread"));
                workerGroup = new EpollEventLoopGroup(rpcServerOptions.getIoThreadNum(),
                        new CustomThreadFactory("server-io-thread"));
            }
            ((EpollEventLoopGroup) bossGroup).setIoRatio(100);
            ((EpollEventLoopGroup) workerGroup).setIoRatio(100);
            bootstrap.channel(EpollServerSocketChannel.class);
            bootstrap.option(EpollChannelOption.EPOLL_MODE, EpollMode.EDGE_TRIGGERED);
            bootstrap.childOption(EpollChannelOption.EPOLL_MODE, EpollMode.EDGE_TRIGGERED);
            log.info("use netty epoll edge trigger mode");
        } else {
            if (rpcServerOptions.isGlobalThreadPoolSharing()) {
                bossGroup = ServerAcceptorThreadPoolInstance.getOrCreateNioInstance(rpcServerOptions.getAcceptorThreadNum());
                workerGroup = ServerIoThreadPoolInstance.getOrCreateNioInstance(rpcServerOptions.getAcceptorThreadNum());
            } else {
                bossGroup = new NioEventLoopGroup(rpcServerOptions.getAcceptorThreadNum(),
                        new CustomThreadFactory("server-acceptor-thread"));
                workerGroup = new NioEventLoopGroup(rpcServerOptions.getIoThreadNum(),
                        new CustomThreadFactory("server-io-thread"));
            }
            ((NioEventLoopGroup) bossGroup).setIoRatio(100);
            ((NioEventLoopGroup) workerGroup).setIoRatio(100);
            bootstrap.channel(NioServerSocketChannel.class);
            log.info("use jdk nio event mode");
        }

        bootstrap.option(ChannelOption.SO_BACKLOG, rpcServerOptions.getBacklog());
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, rpcServerOptions.isKeepAlive());
        bootstrap.childOption(ChannelOption.TCP_NODELAY, rpcServerOptions.isTcpNoDelay());
        bootstrap.childOption(ChannelOption.SO_REUSEADDR, true);
        bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        bootstrap.childOption(ChannelOption.SO_LINGER, rpcServerOptions.getSoLinger());
        bootstrap.childOption(ChannelOption.SO_SNDBUF, rpcServerOptions.getSendBufferSize());
        bootstrap.childOption(ChannelOption.SO_RCVBUF, rpcServerOptions.getReceiveBufferSize());
        final RpcServerHandler rpcServerHandler = new RpcServerHandler(this);
        ChannelInitializer<SocketChannel> initializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(
                        "idleStateAwareHandler", new IdleStateHandler(
                                rpcServerOptions.getReaderIdleTime(),
                                rpcServerOptions.getWriterIdleTime(),
                                rpcServerOptions.getKeepAliveTime()));
                ch.pipeline().addLast("idle", new RpcServerChannelIdleHandler());
                ch.pipeline().addLast(rpcServerHandler);
            }
        };
        bootstrap.group(bossGroup, workerGroup).childHandler(initializer);

        this.serverStatus = new ServerStatus(this);

        // 注册serverPush的内部服务接口
        if (protocol instanceof ServerPushProtocol) {
            timeoutTimer = TimerInstance.getInstance();
            serviceManager.registerService(new RegisterServiceImpl(), threadPool);
        }
    }

    public void registerService(Object service) {
        registerService(service, null, null, null);
    }

    public void registerService(Object service, NamingOptions namingOptions) {
        registerService(service, null, namingOptions, null);
    }

    public void registerService(Object service, Class targetClass, NamingOptions namingOptions) {
        registerService(service, targetClass, namingOptions, null);
    }

    public void registerService(Object service, RpcServerOptions serverOptions) {
        registerService(service, null, null, serverOptions);
    }

    /**
     * register service which can be accessed by client
     *
     * @param service       the service object which implement rpc interface.
     * @param namingOptions register center info
     * @param serverOptions service own custom RpcServerOptions
     *                      if not null, the service will not use the shared thread pool.
     */
    public void registerService(Object service, Class targetClass, NamingOptions namingOptions,
                                RpcServerOptions serverOptions) {
        serviceList.add(service);
        ThreadPool customThreadPool = threadPool;
        if (serverOptions != null) {
            customThreadPool = new ThreadPool(serverOptions.getWorkThreadNum(),
                    new CustomThreadFactory(service.getClass().getSimpleName() + "-work-thread"));
            customThreadPools.add(customThreadPool);
        }

        if (targetClass == null) {
            serviceManager.registerService(service, customThreadPool);
        } else {
            serviceManager.registerService(targetClass, service, customThreadPool);
        }
    }

    public void start() {
        try {
            // 判断是否在jarvis环境，若是jarvis环境则以环境变量port为准，否则以用户自定义的port为准
            if (rpcServerOptions.getJarvisPortName() != null) {
                if (System.getenv(rpcServerOptions.getJarvisPortName()) != null) {
                    this.port = Integer.valueOf(System.getenv(rpcServerOptions.getJarvisPortName()));
                }
            }
            ChannelFuture channelFuture;
            if (null != host) {
                channelFuture = bootstrap.bind(host, port);
            } else {
                channelFuture = bootstrap.bind(port);
            }
            channelFuture.sync();
            if (port == 0 && channelFuture.channel() != null) {
                // update port to the actual value in case the server in started on a random port
                Channel channel = channelFuture.channel();
                SocketAddress localAddress = channel.localAddress();
                if (localAddress instanceof InetSocketAddress) {
                    this.port = ((InetSocketAddress) localAddress).getPort();
                }
            }
        } catch (InterruptedException e) {
            log.error("server failed to start, {}", e.getMessage());
        }
        if (log.isInfoEnabled()) {
            log.info("server started on port={} success", port);
        }
    }

    public boolean shutdown() {
        if (stop.compareAndSet(false, true)) {
            if (bossGroup != null && !rpcServerOptions.isGlobalThreadPoolSharing()) {
                bossGroup.shutdownGracefully().syncUninterruptibly();
            }
            if (workerGroup != null && !rpcServerOptions.isGlobalThreadPoolSharing()) {
                workerGroup.shutdownGracefully().syncUninterruptibly();
            }
            if (threadPool != null && !rpcServerOptions.isGlobalThreadPoolSharing()) {
                threadPool.stop();
            }
            if (CollectionUtils.isNotEmpty(customThreadPools)) {
                log.info("clean customized thread pool");
                for (ThreadPool pool : customThreadPools) {
                    pool.stop();
                }
            }
            return true;
        }
        return false;
    }

    public boolean isShutdown() {
        return stop.get();
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public <T> AsyncAwareFuture<T> sendServerPush(Request request) {
        Channel channel = request.getChannel();
        ChannelInfo orCreateServerChannelInfo = ChannelInfo.getOrCreateServerChannelInfo(channel); // todo
        // create RpcFuture object
        RpcFuture rpcFuture = new ServerPushRpcFuture();
        rpcFuture.setRpcMethodInfo(request.getRpcMethodInfo());
        rpcFuture.setCallback(request.getCallback());
        rpcFuture.setChannelInfo(orCreateServerChannelInfo);
        // generate correlationId
        final long correlationId = PushServerRpcFutureManager.getInstance().putRpcFuture(rpcFuture);

        request.setCorrelationId(correlationId);
        request.getSpHead().setCorrelationId(correlationId);
        // read write timeout
        final long readTimeout = request.getReadTimeoutMillis();
        final long writeTimeout = request.getWriteTimeoutMillis();
        Timeout timeout = timeoutTimer.newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                long timeoutCorrelationId = correlationId;
                PushServerRpcFutureManager rpcFutureManager = PushServerRpcFutureManager.getInstance();
                RpcFuture rpcFuture = rpcFutureManager.removeRpcFuture(timeoutCorrelationId);

                if (rpcFuture != null) {
                    long elapseTime = System.currentTimeMillis() - rpcFuture.getStartTime();
                    String errMsg = String.format("request timeout,correlationId=%d,ip=%s,port=%d,elapse=%dms",
                            timeoutCorrelationId, "?", port, elapseTime);
                    log.info(errMsg);
                    Response response = protocol.createResponse();
                    response.setException(new RpcException(RpcException.TIMEOUT_EXCEPTION, errMsg));
                    response.setRpcFuture(rpcFuture);
                    rpcFuture.handleResponse(response);
                } else {
                    log.error("timeout rpc is missing, correlationId={}", timeoutCorrelationId);
                    throw new RpcException(RpcException.UNKNOWN_EXCEPTION, "timeout rpc is missing");
                }
            }
        }, readTimeout, TimeUnit.MILLISECONDS);

        // set the missing parameters
        rpcFuture.setTimeout(timeout);
        try {
            // netty will release the send buffer after sent.
            // we retain here, so it can be used when rpc retry.
            request.retain();

            ByteBuf byteBuf = protocol.encodeRequest(request);
            ChannelFuture sendFuture = channel.writeAndFlush(byteBuf);
            sendFuture.awaitUninterruptibly(writeTimeout);
            if (!sendFuture.isSuccess()) {
                if (!(sendFuture.cause() instanceof ClosedChannelException)) {
                    log.warn("send request failed, channelActive={}, ex=",
                            channel.isActive(), sendFuture.cause());
                }
                String errMsg = String.format("send request failed, channelActive=%b, ex=%s",
                        channel.isActive(), sendFuture.cause().getMessage());
                throw new RpcException(RpcException.NETWORK_EXCEPTION, errMsg);
            }
        } catch (Exception ex) {
            timeout.cancel();
            if (ex instanceof RpcException) {
                throw (RpcException) ex;
            } else {
                throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, ex.getMessage(), ex);
            }
        }

        return rpcFuture;
    }

    public void execute(Request request, Response response) throws RpcException {
        new ServerInvokeInterceptor().aroundProcess(request, response, null);
    }

}
