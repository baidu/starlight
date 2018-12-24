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
import com.baidu.brpc.naming.NamingService;
import com.baidu.brpc.naming.RegisterInfo;
import com.baidu.brpc.protocol.Protocol;
import com.baidu.brpc.utils.NetUtils;
import com.baidu.brpc.utils.ThreadPool;
import lombok.Getter;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.brpc.protocol.ProtocolManager;
import com.baidu.brpc.server.handler.RpcServerChannelIdleHandler;
import com.baidu.brpc.server.handler.RpcServerHandler;
import com.baidu.brpc.utils.CustomThreadFactory;
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
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * Created by wenweihu86 on 2017/4/24.
 */
@Getter
public class RpcServer {
    private static final Logger LOG = LoggerFactory.getLogger(RpcServer.class);
    private RpcServerOptions rpcServerOptions = new RpcServerOptions();
    // 端口
    private int port;
    // netty bootstrap
    private ServerBootstrap bootstrap;
    // netty acceptor thread pool
    private EventLoopGroup bossGroup;
    // netty io thread pool
    private EventLoopGroup workerGroup;
    private List<Interceptor> interceptors = new ArrayList<Interceptor>();
    private Protocol protocol;
    private ThreadPool threadPool;
    private NamingService namingService;
    private List<Object> serviceList = new ArrayList<Object>();
    private List<RegisterInfo> registerInfoList = new ArrayList<RegisterInfo>();
    private ServerStatus serverStatus;
    private RpcServer metaHttpServer;

    public RpcServer(int port) {
        this(port, new RpcServerOptions(), null);
    }

    public RpcServer(int port, RpcServerOptions options) {
        this(port, options, null, null);
    }

    public RpcServer(int port, RpcServerOptions options, List<Interceptor> interceptors) {
        this(port, options, interceptors, null);
    }

    public RpcServer(int port,
                     final RpcServerOptions options,
                     List<Interceptor> interceptors,
                     NamingService namingService) {
        this.port = port;
        if (options != null) {
            try {
                BeanUtils.copyProperties(this.rpcServerOptions, options);
            } catch (Exception ex) {
                LOG.warn("init options failed, so use default");
            }
        }
        if (interceptors != null) {
            this.interceptors = interceptors;
        }
        this.namingService = namingService;
        // init protocol
        ProtocolManager.instance().init(this.rpcServerOptions.getEncoding());
        if (rpcServerOptions.getProtocolType() != null) {
            this.protocol = ProtocolManager.instance().getProtocol(rpcServerOptions.getProtocolType());
        }
        this.threadPool = new ThreadPool(rpcServerOptions.getWorkThreadNum(),
                new CustomThreadFactory("server-work-thread"));
        bootstrap = new ServerBootstrap();
        if (Epoll.isAvailable()) {
            bossGroup = new EpollEventLoopGroup(
                    rpcServerOptions.getAcceptorThreadNum(),
                    new CustomThreadFactory("server-acceptor-thread"));
            workerGroup = new EpollEventLoopGroup(
                    rpcServerOptions.getIoThreadNum(),
                    new CustomThreadFactory("server-io-thread"));
            ((EpollEventLoopGroup) bossGroup).setIoRatio(100);
            ((EpollEventLoopGroup) workerGroup).setIoRatio(100);
            bootstrap.channel(EpollServerSocketChannel.class);
            bootstrap.option(EpollChannelOption.EPOLL_MODE, EpollMode.EDGE_TRIGGERED);
            bootstrap.childOption(EpollChannelOption.EPOLL_MODE, EpollMode.EDGE_TRIGGERED);
            LOG.info("use epoll edge trigger mode");
        } else {
            bossGroup = new NioEventLoopGroup(
                    rpcServerOptions.getAcceptorThreadNum(),
                    new CustomThreadFactory("server-acceptor-thread"));
            workerGroup = new NioEventLoopGroup(
                    rpcServerOptions.getIoThreadNum(),
                    new CustomThreadFactory("server-io-thread"));
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
                if (rpcServerOptions.isHttp()) {
                    ch.pipeline().addLast(new HttpServerCodec());
                    ch.pipeline().addLast(new HttpObjectAggregator(10 * 1024 * 1024));
                }
                ch.pipeline().addLast(rpcServerHandler);
            }
        };
        bootstrap.group(bossGroup, workerGroup).childHandler(initializer);

        if (this.rpcServerOptions.getMetaHttpPort() > 0) {
            this.serverStatus = new ServerStatus(this);
            if (!this.rpcServerOptions.isHttp()) {
                RpcServerOptions httpOptions = new RpcServerOptions();
                try {
                    BeanUtils.copyProperties(httpOptions, this.rpcServerOptions);
                } catch (Exception ex) {
                    LOG.warn("copy rpc server options failed");
                }
                httpOptions.setHttp(true);
                httpOptions.setWorkThreadNum(1);
                httpOptions.setIoThreadNum(1);
                int metaHttpPort = this.rpcServerOptions.getMetaHttpPort();
                metaHttpServer = new RpcServer(metaHttpPort, httpOptions);
            }
        }
    }

    public void registerService(Object service) {
        ServiceManager serviceManager = ServiceManager.getInstance();
        serviceManager.registerService(service);
        serviceList.add(service);
        RegisterInfo registerInfo = new RegisterInfo(
                NetUtils.getLocalAddress().getHostAddress(),
                port,
                service.getClass().getName(),
                rpcServerOptions.getNamingServiceGroup(),
                rpcServerOptions.getNamingServiceVersion());
        registerInfoList.add(registerInfo);
    }

    public void start() {
        try {
            // 判断是否在jarvis环境，若是jarvis环境则以环境变量port为准，否则以用户自定义的port为准
            if (rpcServerOptions.getJarvisPortName() != null) {
                if (System.getenv(rpcServerOptions.getJarvisPortName()) != null) {
                    port = Integer.valueOf(System.getenv(rpcServerOptions.getJarvisPortName()));
                }
            }
            ChannelFuture channelFuture = bootstrap.bind(port);
            channelFuture.sync();
            if (namingService != null) {
                for (RegisterInfo registerInfo : registerInfoList) {
                    namingService.register(registerInfo);
                }
            }
            if (rpcServerOptions.getMetaHttpPort() > 0 && !rpcServerOptions.isHttp()) {
                metaHttpServer.start();
            }
        } catch (InterruptedException e) {
            LOG.error("server failed to start, {}", e.getMessage());
        }
        LOG.info("server started on port={} success", port);
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
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (threadPool != null) {
            threadPool.stop();
        }
        if (rpcServerOptions.getMetaHttpPort() > 0 && !rpcServerOptions.isHttp()) {
            metaHttpServer.shutdown();
        }
    }

}
