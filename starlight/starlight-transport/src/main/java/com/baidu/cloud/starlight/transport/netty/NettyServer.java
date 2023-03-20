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
 
package com.baidu.cloud.starlight.transport.netty;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.common.URI;
import com.baidu.cloud.starlight.api.exception.TransportException;
import com.baidu.cloud.starlight.api.extension.ExtensionLoader;
import com.baidu.cloud.starlight.api.model.RpcResponse;
import com.baidu.cloud.starlight.api.protocol.Protocol;
import com.baidu.cloud.starlight.api.rpc.Processor;
import com.baidu.cloud.starlight.api.transport.PeerStatus;
import com.baidu.cloud.starlight.api.transport.ServerPeer;
import com.baidu.cloud.starlight.api.transport.channel.RpcChannel;
import com.baidu.cloud.starlight.api.utils.EnvUtils;
import com.baidu.cloud.starlight.protocol.stargate.StargateProtocol;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SingleThreadEventLoop;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollMode;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by liuruisen on 2020/2/3.
 */
public class NettyServer implements ServerPeer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyServer.class);

    private Processor processor;

    private URI uri;

    private ServerBootstrap bootstrap;

    /**
     * netty acceptor thread pool
     */
    private EventLoopGroup parentGroup;

    /**
     * netty io thread pool
     */
    private EventLoopGroup childGroup;

    private Channel serverChannel;

    private Map<String, RpcChannel> rpcChannels;

    private volatile PeerStatus status;

    private DirectMemoryReporter reporter;

    public NettyServer(URI uri) {
        this.uri = uri;
        this.rpcChannels = new ConcurrentHashMap<>();
    }

    @Override
    public void init() {

        bootstrap = new ServerBootstrap();
        int acceptThreadNum = uri.getParameter(Constants.ACCEPT_THREADS_KEY, Constants.DEFAULT_ACCEPTOR_THREAD_VALUE);
        int ioThreadNum = uri.getParameter(Constants.IO_THREADS_KEY, Constants.DEFAULT_IO_THREADS_VALUE);
        int ioRatio = uri.getParameter(Constants.NETTY_IO_RATIO_KEY, Constants.DEFAULT_NETTY_IO_RATIO);

        if (Epoll.isAvailable()) {
            parentGroup = new EpollEventLoopGroup(acceptThreadNum,
                new DefaultThreadFactory(Constants.SERVER_EPOLL_ACCEPT_THREAD_NAME_PREFIX, false));
            childGroup = new EpollEventLoopGroup(ioThreadNum,
                new DefaultThreadFactory(Constants.SERVER_EPOLL_THREAD_NAME_PREFIX, false));
            ((EpollEventLoopGroup) parentGroup).setIoRatio(ioRatio);
            ((EpollEventLoopGroup) childGroup).setIoRatio(ioRatio);
            bootstrap.channel(EpollServerSocketChannel.class);
            bootstrap.option(EpollChannelOption.EPOLL_MODE, EpollMode.EDGE_TRIGGERED);
            bootstrap.childOption(EpollChannelOption.EPOLL_MODE, EpollMode.EDGE_TRIGGERED);
            LOGGER.info("NettyServer use epoll mode.");
        } else {
            parentGroup = new NioEventLoopGroup(acceptThreadNum,
                new DefaultThreadFactory(Constants.SERVER_NIO_ACCEPT_THREAD_NAME_PREFIX, false));
            childGroup = new NioEventLoopGroup(ioThreadNum,
                new DefaultThreadFactory(Constants.SERVER_NIO_THREAD_NAME_PREFIX, false));
            ((NioEventLoopGroup) parentGroup).setIoRatio(ioRatio);
            ((NioEventLoopGroup) childGroup).setIoRatio(ioRatio);
            bootstrap.channel(NioServerSocketChannel.class);
            LOGGER.info("NettyServer use Nio mode.");
        }
        bootstrap.group(parentGroup, childGroup);

        // config
        bootstrap.childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);
        bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        bootstrap.childOption(ChannelOption.SO_REUSEADDR, Boolean.TRUE);
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
        bootstrap.option(ChannelOption.SO_BACKLOG, Constants.SO_BACKLOG);
        bootstrap.childOption(ChannelOption.SO_LINGER, Constants.SO_LINGER);
        bootstrap.childOption(ChannelOption.SO_SNDBUF, Constants.SO_SNDBUF);
        bootstrap.childOption(ChannelOption.SO_RCVBUF, Constants.SO_REVBUF);

        final ChannelInitializer<SocketChannel> initializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new DecoderHandler()); // inbound 1 DecoderHandler
                if (getUri().getParameter(Constants.CONNECT_KEEPALIVE_ENABLED_KEY,
                    Constants.CONNECT_KEEPALIVE_ENABLED_VALUE)) {
                    ch.pipeline()
                        .addLast(new IdleStateHandler(0, 0,
                            getUri().getParameter(Constants.ALL_IDLE_TIMEOUT_KEY, Constants.ALL_IDLE_TIMEOUT_VALUE),
                            TimeUnit.SECONDS))
                        .addLast(new HeartbeatHandler()); // inbound 2 heartbeatHandler
                }
                ch.pipeline().addLast(new RpcHandler(NettyServer.this)) // inbound 3 RpcHandler
                    .addLast(new EncoderHandler()); // outbound EncoderHandler
            }
        };
        bootstrap.childHandler(initializer);
    }

    @Override
    public void bind() {
        // bind
        ChannelFuture channelFuture = bootstrap.bind(getUri().getHost(), getUri().getPort());
        channelFuture.syncUninterruptibly();
        if (!channelFuture.isSuccess()) {
            throw new TransportException(TransportException.BIND_EXCEPTION,
                "Server bind to ip {" + getUri().getHost() + "}, port {" + getUri().getPort() + "} failed",
                channelFuture.cause());
        }
        LOGGER.info("Starlight server bind to ip {} port {}", getUri().getHost(), getUri().getPort());
        serverChannel = channelFuture.channel();
        // 测试用，上线不开启
        if (!EnvUtils.isJarvisOnline()) {
            reporter = new DirectMemoryReporter();
        }
        updateStatus(new PeerStatus(PeerStatus.Status.ACTIVE, System.currentTimeMillis()));
    }

    @Override
    public boolean isBound() {
        return serverChannel.isOpen();
    }

    @Override
    public URI getUri() {
        return uri;
    }

    @Override
    public void close() {
        try {
            if (serverChannel != null) {
                serverChannel.close();
            }

            if (processor != null) {
                processor.close();
            }

            if (reporter != null) {
                reporter.close();
            }
        } finally {
            if (parentGroup != null) {
                parentGroup.shutdownGracefully();
            }
            if (childGroup != null) {
                childGroup.shutdownGracefully();
            }
        }
    }

    @Override
    public void gracefullyShutdown(long quietPeriod, long timeout) {
        LOGGER.info("Shutdown starlight server gracefully start");
        try {
            // send shutdown event to all connected channels to notify the server is shutting down
            LOGGER.info("Notify server shutting down to clients begin");
            long startTime = System.currentTimeMillis();
            for (Map.Entry<String, RpcChannel> entry : rpcChannels.entrySet()) {
                try {
                    RpcResponse shuttingDownEvent = shuttingDownEvent(StargateProtocol.PROTOCOL_NAME);
                    Protocol protocol =
                        ExtensionLoader.getInstance(Protocol.class).getExtension(StargateProtocol.PROTOCOL_NAME);
                    protocol.getEncoder().encodeBody(shuttingDownEvent);

                    entry.getValue().send(shuttingDownEvent);
                    LOGGER.info("Notify server shutting to {}", entry.getValue().channel().remoteAddress());
                } catch (TransportException e) {
                    // ignore and log
                    LOGGER.warn("Notify client SHUTTING_DOWN failed, remoteAddress: {}", entry.getKey(), e);
                }
            }
            LOGGER.info("Notify server shutting down end, cost {} clients {}", System.currentTimeMillis() - startTime,
                rpcChannels.size());

            long shutdownTimeoutTime = System.currentTimeMillis() + timeout * 1000;

            // wait for quiet period pass, in this period server can receive little request and handle them
            if (quietPeriod > 0) {
                LOGGER.info("Wait for quiet period pass {}s", quietPeriod);
                try {
                    TimeUnit.SECONDS.sleep(quietPeriod);
                } catch (InterruptedException e) {
                    // ignore
                }
            }

            // set shutting down, server cannot handle any request
            updateStatus(new PeerStatus(PeerStatus.Status.SHUTTING_DOWN, System.currentTimeMillis()));

            // unbind, to prevent new connect
            if (serverChannel != null) {
                serverChannel.close();
            }

            // execute unfinished task and wait for the timeout to arrive
            if (timeout > 0) {
                for (;;) {
                    if (pendingTaskNum().equals(0) && getProcessor().allWaitTaskCount().equals(0)) { // whether the
                                                                                                     // requests is
                                                                                                     // completed
                        LOGGER.info("NettyServer has processed all requests, gracefully shutdown.");
                        break;
                    }

                    if (System.currentTimeMillis() >= shutdownTimeoutTime) {
                        LOGGER.info("NettyServer reach the maximum timeout time, force shutdown. "
                            + "Number of unfinished request {}", getProcessor().allWaitTaskCount());
                        break;
                    }

                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
            }
            // execute processor shutdown & netty gracefully shutdown
            if (processor != null) {
                processor.close(); // the thread pool will shutdown now
            }
            updateStatus(new PeerStatus(PeerStatus.Status.SHUTDOWN, System.currentTimeMillis()));
        } catch (Throwable e) {
            LOGGER.warn("Shutdown starlight server gracefully failed, cause by: ", e);
        } finally {
            if (parentGroup != null) {
                parentGroup.shutdownGracefully();
            }
            if (childGroup != null) {
                childGroup.shutdownGracefully();
            }
        }
        LOGGER.info("Shutdown starlight server gracefully end");
    }

    @Override
    public void setProcessor(Processor processor) {
        this.processor = processor;
    }

    @Override
    public Processor getProcessor() {
        return processor;
    }

    @Override
    public Map<String, RpcChannel> rpcChannels() {
        return this.rpcChannels;
    }

    @Override
    public PeerStatus status() {
        return this.status;
    }

    @Override
    public void updateStatus(PeerStatus status) {
        // server update status no need consider thread safe
        this.status = status;
    }

    private Integer pendingTaskNum() {
        Integer pendingTaskNum = 0;
        try {
            for (EventExecutor eventExecutor : parentGroup) {
                if (eventExecutor instanceof SingleThreadEventLoop) {
                    pendingTaskNum += ((SingleThreadEventLoop) eventExecutor).pendingTasks();
                }
            }
            LOGGER.debug("Parent event loop group pending task num {}", pendingTaskNum);

            for (EventExecutor eventExecutor : childGroup) {
                if (eventExecutor instanceof SingleThreadEventLoop) {
                    pendingTaskNum += ((SingleThreadEventLoop) eventExecutor).pendingTasks();
                }
            }
            LOGGER.debug("Parent and child event loop group pending task num {}", pendingTaskNum);

            pendingTaskNum += GlobalEventExecutor.INSTANCE.pendingTasks();

            LOGGER.debug("Parent and child and global event loop group pending task num {}", pendingTaskNum);
        } catch (Throwable e) {
            LOGGER.warn("Calculate netty pending task count failed, caused by ", e);
        }

        LOGGER.info("Netty pending tasks num is {}", pendingTaskNum);
        return pendingTaskNum;
    }
}
