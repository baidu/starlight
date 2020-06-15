package com.baidu.brpc.client.channel;

import com.baidu.brpc.client.CommunicationOptions;
import com.baidu.brpc.client.handler.IdleChannelHandler;
import com.baidu.brpc.client.handler.RpcClientHandler;
import com.baidu.brpc.thread.BrpcThreadPoolManager;
import com.baidu.brpc.utils.BrpcConstants;
import com.baidu.brpc.utils.ThreadPool;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollMode;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

@Slf4j
public class BootstrapManager {
    private ConcurrentMap<String, Bootstrap> bootstrapMap = new ConcurrentHashMap<String, Bootstrap>();

    private static volatile BootstrapManager instance;

    public static BootstrapManager getInstance() {
        if (instance == null) {
            synchronized(BootstrapManager.class) {
                if (instance == null) {
                    instance = new BootstrapManager();
                }
            }
        }
        return instance;
    }

    public Bootstrap getOrCreateBootstrap(String serviceName, final CommunicationOptions communicationOptions) {
        Bootstrap bootstrap = bootstrapMap.get(serviceName);
        if (bootstrap != null) {
            return bootstrap;
        }
        bootstrap = createBooStrap(serviceName, communicationOptions);
        Bootstrap prev = bootstrapMap.putIfAbsent(serviceName, bootstrap);
        if (prev != null) {
            log.warn("prev bootstrap exist for service:{}", serviceName);
            if (!communicationOptions.isGlobalThreadPoolSharing()) {

            }
        }
        return bootstrap;
    }

    public Bootstrap createBooStrap(String serviceName, final CommunicationOptions communicationOptions) {
        // init netty bootstrap
        Bootstrap bootstrap = new Bootstrap();
        if (communicationOptions.getIoEventType() == BrpcConstants.IO_EVENT_NETTY_EPOLL) {
            bootstrap.channel(EpollSocketChannel.class);
            bootstrap.option(EpollChannelOption.EPOLL_MODE, EpollMode.EDGE_TRIGGERED);
        } else {
            bootstrap.channel(NioSocketChannel.class);
        }

        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, communicationOptions.getConnectTimeoutMillis());
        bootstrap.option(ChannelOption.SO_KEEPALIVE, communicationOptions.isKeepAlive());
        bootstrap.option(ChannelOption.SO_REUSEADDR, communicationOptions.isReuseAddr());
        bootstrap.option(ChannelOption.TCP_NODELAY, communicationOptions.isTcpNoDelay());
        bootstrap.option(ChannelOption.SO_RCVBUF, communicationOptions.getReceiveBufferSize());
        bootstrap.option(ChannelOption.SO_SNDBUF, communicationOptions.getSendBufferSize());

        BrpcThreadPoolManager threadPoolManager = BrpcThreadPoolManager.getInstance();
        boolean isSharing = communicationOptions.isGlobalThreadPoolSharing();
        ThreadPool workThreadPool = threadPoolManager.getOrCreateClientWorkThreadPool(
                serviceName, isSharing, communicationOptions.getWorkThreadNum());
        ExecutorService exceptionThreadPool = threadPoolManager.getExceptionThreadPool();
        final RpcClientHandler rpcClientHandler = new RpcClientHandler(workThreadPool, exceptionThreadPool);
        final ChannelInitializer<SocketChannel> initializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                if (communicationOptions.getChannelType() == ChannelType.SINGLE_CONNECTION) {
                    ch.pipeline().addLast(new IdleStateHandler(
                            0, 0, communicationOptions.getKeepAliveTime()));
                    ch.pipeline().addLast(new IdleChannelHandler());
                }
                ch.pipeline().addLast(rpcClientHandler);
            }
        };

        EventLoopGroup ioThreadPool = threadPoolManager.getOrCreateClientIoThreadPool(
                serviceName, isSharing, communicationOptions.getIoThreadNum(), communicationOptions.getIoEventType());
        bootstrap.group(ioThreadPool).handler(initializer);
        return bootstrap;
    }

    public Bootstrap removeBootstrap(String serviceName) {
        return bootstrapMap.remove(serviceName);
    }

    public void removeAll() {
        bootstrapMap.clear();
    }
}
