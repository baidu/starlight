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

package com.baidu.brpc.server;

import java.util.ArrayList;
import java.util.List;

import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.interceptor.ServerInvokeInterceptor;
import com.baidu.brpc.naming.BrpcURL;
import com.baidu.brpc.naming.NamingOptions;
import com.baidu.brpc.naming.NamingService;
import com.baidu.brpc.naming.NamingServiceFactory;
import com.baidu.brpc.naming.RegisterInfo;
import com.baidu.brpc.protocol.Protocol;
import com.baidu.brpc.protocol.ProtocolManager;
import com.baidu.brpc.server.handler.RpcServerChannelIdleHandler;
import com.baidu.brpc.server.handler.RpcServerHandler;
import com.baidu.brpc.thread.BrpcIoThreadPoolInstance;
import com.baidu.brpc.thread.BrpcWorkThreadPoolInstance;
import com.baidu.brpc.thread.ShutDownManager;
import com.baidu.brpc.utils.CollectionUtils;
import com.baidu.brpc.utils.CustomThreadFactory;
import com.baidu.brpc.utils.NetUtils;
import com.baidu.brpc.utils.ThreadPool;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollMode;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TCP Rpc Server
 *
 * @author wenweihu86
 * @author guohao02
 */
@Getter
public class RpcServer {
    private static final Logger LOG = LoggerFactory.getLogger(RpcServer.class);
    private RpcServerOptions rpcServerOptions = new RpcServerOptions();

    /**
     * host to bind
     */
    private String host;
    /**
     * port to bind
     */
    private int port;
    /**
     * netty bootstrap
     */
    private ServerBootstrap bootstrap;
    /**
     * netty acceptor thread pool
     */
    private EventLoopGroup bossGroup;
    /**
     * netty io thread pool
     */
    private EventLoopGroup workerGroup;
    private List<Interceptor> interceptors = new ArrayList<Interceptor>();
    private Protocol protocol;
    private ThreadPool threadPool;
    private List<ThreadPool> customThreadPools = new ArrayList<ThreadPool>();
    private NamingService namingService;
    private List<Object> serviceList = new ArrayList<Object>();
    private List<RegisterInfo> registerInfoList = new ArrayList<RegisterInfo>();
    private ServerStatus serverStatus;

    public RpcServer(int port) {
        this(null, port, new RpcServerOptions(), null, null);
    }

    public RpcServer(int port, RpcServerOptions options) {
        this(null, port, options, null, null);
    }

    public RpcServer(String host, int port) {
        this(host, port, new RpcServerOptions(), null, null);
    }

    public RpcServer(int port, RpcServerOptions options, List<Interceptor> interceptors) {
        this(null, port, options, interceptors);
    }

    public RpcServer(String host, int port, RpcServerOptions options) {
        this(host, port, options, null, null);
    }

    public RpcServer(String host, int port, RpcServerOptions options, List<Interceptor> interceptors) {
        this(host, port, options, interceptors, null);
    }

    public RpcServer(int port, RpcServerOptions options, NamingServiceFactory namingServiceFactory) {
        this(null, port, options, null, namingServiceFactory);
    }

    public RpcServer(String host, int port, RpcServerOptions options, NamingServiceFactory namingServiceFactory) {
        this(host, port, options, null, namingServiceFactory);
    }

    public RpcServer(int port,
                     final RpcServerOptions options,
                     List<Interceptor> interceptors,
                     NamingServiceFactory namingServiceFactory) {
        this(null, port, options, interceptors, namingServiceFactory);
    }

    public RpcServer(String host, int port,
                     final RpcServerOptions options,
                     List<Interceptor> interceptors,
                     NamingServiceFactory namingServiceFactory) {
        this.host = host;
        this.port = port;
        if (options != null) {
            try {
                this.rpcServerOptions.copyFrom(options);
            } catch (Exception ex) {
                LOG.warn("init options failed, so use default");
            }
        }
        if (interceptors != null) {
            this.interceptors.addAll(interceptors);
        }
        if (namingServiceFactory != null
                && StringUtils.isNotBlank(rpcServerOptions.getNamingServiceUrl())) {
            BrpcURL url = new BrpcURL(rpcServerOptions.getNamingServiceUrl());
            this.namingService = namingServiceFactory.createNamingService(url);
        }
        // init protocol
        ProtocolManager.instance().init(this.rpcServerOptions.getEncoding());
        if (rpcServerOptions.getProtocolType() != null) {
            this.protocol = ProtocolManager.instance().getProtocol(rpcServerOptions.getProtocolType());
        }

        // shutDownManager init once
        ShutDownManager.getInstance();

        threadPool = BrpcWorkThreadPoolInstance.getOrCreateInstance(rpcServerOptions.getWorkThreadNum());
        workerGroup = BrpcIoThreadPoolInstance.getOrCreateInstance(rpcServerOptions.getIoThreadNum());

        bootstrap = new ServerBootstrap();

        if (Epoll.isAvailable()) {

            bossGroup = new EpollEventLoopGroup(rpcServerOptions.getAcceptorThreadNum(),
                    new CustomThreadFactory("server-acceptor-thread"));

            ((EpollEventLoopGroup) bossGroup).setIoRatio(100);
            ((EpollEventLoopGroup) workerGroup).setIoRatio(100);
            bootstrap.channel(EpollServerSocketChannel.class);
            bootstrap.option(EpollChannelOption.EPOLL_MODE, EpollMode.EDGE_TRIGGERED);
            bootstrap.childOption(EpollChannelOption.EPOLL_MODE, EpollMode.EDGE_TRIGGERED);
            LOG.info("use epoll edge trigger mode");
        } else {

            bossGroup = new NioEventLoopGroup(rpcServerOptions.getAcceptorThreadNum(),
                    new CustomThreadFactory("server-acceptor-thread"));

            ((NioEventLoopGroup) bossGroup).setIoRatio(100);
            ((NioEventLoopGroup) workerGroup).setIoRatio(100);
            bootstrap.channel(NioServerSocketChannel.class);
            LOG.info("use normal mode");
        }

        bootstrap.option(ChannelOption.SO_BACKLOG, rpcServerOptions.getBacklog());
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, rpcServerOptions.isKeepAlive());
        bootstrap.childOption(ChannelOption.TCP_NODELAY, rpcServerOptions.isTcpNoDelay());
        bootstrap.childOption(ChannelOption.SO_REUSEADDR, true);
        bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        bootstrap.childOption(ChannelOption.SO_LINGER, rpcServerOptions.getSoLinger());
        bootstrap.childOption(ChannelOption.SO_SNDBUF, rpcServerOptions.getSendBufferSize());
        bootstrap.childOption(ChannelOption.SO_RCVBUF, rpcServerOptions.getReceiveBufferSize());
        final RpcServerHandler rpcServerHandler = new RpcServerHandler(RpcServer.this);
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
    }

    public void registerService(Object service) {
        registerService(service, null, null);
    }

    public void registerService(Object service, NamingOptions namingOptions) {
        registerService(service, namingOptions, null);
    }

    public void registerService(Object service, RpcServerOptions serverOptions) {
        registerService(service, null, serverOptions);
    }

    /**
     * register service which can be accessed by client
     * @param service the service object which implement rpc interface.
     * @param namingOptions register center info
     * @param serverOptions service own custom RpcServerOptions
     *                      if not null, the service will not use the shared thread pool.
     */
    public void registerService(Object service, NamingOptions namingOptions, RpcServerOptions serverOptions) {
        serviceList.add(service);
        RegisterInfo registerInfo = new RegisterInfo();
        registerInfo.setService(service.getClass().getInterfaces()[0].getName());
        registerInfo.setHost(NetUtils.getLocalAddress().getHostAddress());
        registerInfo.setPort(port);
        if (namingOptions != null) {
            registerInfo.setGroup(namingOptions.getGroup());
            registerInfo.setVersion(namingOptions.getVersion());
            registerInfo.setIgnoreFailOfNamingService(namingOptions.isIgnoreFailOfNamingService());
        }
        ServiceManager serviceManager = ServiceManager.getInstance();
        ThreadPool customThreadPool = threadPool;
        if (serverOptions != null) {
            customThreadPool = new ThreadPool(serverOptions.getWorkThreadNum(),
                    new CustomThreadFactory(service.getClass().getSimpleName() + "-work-thread"));
            customThreadPools.add(customThreadPool);
        }
        serviceManager.registerService(service, customThreadPool);
        registerInfoList.add(registerInfo);
    }

    public void start() {
        this.interceptors.add(new ServerInvokeInterceptor());
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
            if (namingService != null) {
                for (RegisterInfo registerInfo : registerInfoList) {
                    namingService.register(registerInfo);
                }
            }
        } catch (InterruptedException e) {
            LOG.error("server failed to start, {}", e.getMessage());
        }
        if (LOG.isInfoEnabled()) {
            LOG.info("server started on port={} success", port);
        }
    }

    public void shutdown() {
        if (namingService != null) {
            for (RegisterInfo registerInfo : registerInfoList) {
                namingService.unregister(registerInfo);
            }
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }

        if (CollectionUtils.isNotEmpty(customThreadPools)) {
            LOG.info("clean customized ThreadPool");
            for (ThreadPool pool : customThreadPools) {
                pool.stop();
            }
        }
    }

}
