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
import com.baidu.cloud.starlight.api.exception.StarlightRpcException;
import com.baidu.cloud.starlight.api.exception.TransportException;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.rpc.Processor;
import com.baidu.cloud.starlight.api.rpc.RpcContext;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import com.baidu.cloud.starlight.api.transport.ClientPeer;
import com.baidu.cloud.starlight.api.transport.PeerStatus;
import com.baidu.cloud.starlight.api.transport.channel.RpcChannelGroup;
import com.baidu.cloud.starlight.transport.channel.PooledRpcChannelGroup;
import com.baidu.cloud.starlight.api.transport.channel.RpcChannel;
import com.baidu.cloud.starlight.transport.channel.SingleRpcChannelGroup;
import com.baidu.cloud.starlight.transport.utils.TimerHolder;
import com.baidu.cloud.thirdparty.netty.bootstrap.Bootstrap;
import com.baidu.cloud.thirdparty.netty.buffer.PooledByteBufAllocator;
import com.baidu.cloud.thirdparty.netty.channel.ChannelInitializer;
import com.baidu.cloud.thirdparty.netty.channel.ChannelOption;
import com.baidu.cloud.thirdparty.netty.channel.EventLoopGroup;
import com.baidu.cloud.thirdparty.netty.channel.epoll.Epoll;
import com.baidu.cloud.thirdparty.netty.channel.epoll.EpollChannelOption;
import com.baidu.cloud.thirdparty.netty.channel.epoll.EpollEventLoopGroup;
import com.baidu.cloud.thirdparty.netty.channel.epoll.EpollMode;
import com.baidu.cloud.thirdparty.netty.channel.epoll.EpollSocketChannel;
import com.baidu.cloud.thirdparty.netty.channel.nio.NioEventLoopGroup;
import com.baidu.cloud.thirdparty.netty.channel.socket.SocketChannel;
import com.baidu.cloud.thirdparty.netty.channel.socket.nio.NioSocketChannel;
import com.baidu.cloud.thirdparty.netty.handler.timeout.IdleStateHandler;
import com.baidu.cloud.thirdparty.netty.util.Timeout;
import com.baidu.cloud.thirdparty.netty.util.TimerTask;
import com.baidu.cloud.thirdparty.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

/**
 * Created by liuruisen on 2020/2/3.
 */
public class NettyClient implements ClientPeer {
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyClient.class);

    private RpcChannelGroup rpcChannelGroup;

    private Bootstrap bootstrap;

    private Processor processor;

    private static volatile EventLoopGroup eventLoopGroup; // static: shared by all NettyClient

    private URI uri;

    private volatile PeerStatus status;

    // 存储NettyClient这个类的实例信息，以ip:port为key
    private static final Set<String> INSTANCE_SET = new CopyOnWriteArraySet<>();

    // init eventLoopGroup
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (eventLoopGroup == null) {
                return;
            }

            if (INSTANCE_SET.isEmpty()) {
                LOGGER.info("All the instance of NettyClient is closed, will shutdown eventloop gracefully");
                eventLoopGroup.shutdownGracefully();
            }

            long beginShutdownTime = System.currentTimeMillis();

            for (;;) {

                if (INSTANCE_SET.isEmpty()) {
                    LOGGER.info("All the instance of NettyClient is closed, will shutdown eventloop gracefully");
                    eventLoopGroup.shutdownGracefully();
                    break;
                }

                if ((System.currentTimeMillis() - beginShutdownTime) > 1000 * 60 * 2) { // max wait 2min
                    LOGGER.info("Reach the max shutdown time, will shutdown enventloop gracefully");
                    eventLoopGroup.shutdownGracefully();
                    break;
                }

                try {
                    TimeUnit.MILLISECONDS.sleep(1000);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }));
    }

    // Uri: Ip:port + config
    public NettyClient(URI uri) {
        if (eventLoopGroup == null) {
            synchronized (this) {
                if (eventLoopGroup == null) {
                    int ioThreadNum = uri.getParameter(Constants.IO_THREADS_KEY, Constants.DEFAULT_IO_THREADS_VALUE);
                    int ioRatio = uri.getParameter(Constants.NETTY_IO_RATIO_KEY, Constants.DEFAULT_NETTY_IO_RATIO);
                    if (Epoll.isAvailable()) {
                        eventLoopGroup = new EpollEventLoopGroup(ioThreadNum,
                            new DefaultThreadFactory(Constants.CLIENT_EPOLL_THREAD_NAME_PREFIX, true));
                        ((EpollEventLoopGroup) eventLoopGroup).setIoRatio(ioRatio);
                    } else {
                        eventLoopGroup = new NioEventLoopGroup(ioThreadNum,
                            new DefaultThreadFactory(Constants.CLIENT_NIO_THREAD_NAME_PREFIX, true));
                        ((NioEventLoopGroup) eventLoopGroup).setIoRatio(ioRatio);
                    }
                }
            }
        }

        this.uri = uri;
        INSTANCE_SET.add(this.uri.getAddress());
        this.updateNettyResourceMetaHome();
    }

    @Override
    public RpcChannelGroup getChannelGroup() {
        return this.rpcChannelGroup;
    }

    @Override
    public void connect() {
        // 在StarlightClient中调用，且StarlightClient中设置了并发保护，此处暂未考虑并发问题
        rpcChannelGroup = rpcChannelGroup(
            getUri().getParameter(Constants.RPC_CHANNEL_TYPE_KEY, Constants.DEFAULT_RPC_CHANNEL_TYPE_VALUE));
        rpcChannelGroup.init();
    }

    @Override
    public void request(Request request, RpcCallback callback) throws TransportException {
        if (rpcChannelGroup == null) {
            throw new TransportException("RpcChannelGroup of NettyClient is null, plz check");
        }
        RpcChannel rpcChannel = rpcChannelGroup.getRpcChannel();
        try {
            int requestTimeoutMills = Constants.REQUEST_TIMEOUT_VALUE; // default

            if (request.getServiceConfig() != null && request.getServiceConfig().getInvokeTimeoutMills() != null
                && request.getServiceConfig().getInvokeTimeoutMills() > 0) {
                requestTimeoutMills = request.getServiceConfig().getInvokeTimeoutMills();
            }

            if (RpcContext.getContext().getRequestTimeoutMills() != null
                && RpcContext.getContext().getRequestTimeoutMills() > 0) {
                requestTimeoutMills = RpcContext.getContext().getRequestTimeoutMills();
            }
            // carry request time out to server, remember not set it in server side RpcContext
            request.getAttachmentKv().put(Constants.REQUEST_TIMEOUT_KEY, requestTimeoutMills);

            Timeout timeout = TimerHolder.getTimer().newTimeout(new TimerTask() {
                @Override
                public void run(Timeout timeout) throws Exception {
                    RpcCallback rpcCallback = rpcChannel.removeCallback(request.getId());
                    if (rpcCallback == null) {
                        return;
                    }
                    rpcCallback.onError(StarlightRpcException.timeoutException(request, getUri().getAddress()));
                }
            }, requestTimeoutMills, TimeUnit.MILLISECONDS);
            callback.addTimeout(timeout);
            rpcChannel.putCallback(request.getId(), callback); // put callback to rpc channel
            rpcChannel.send(request); // send request
        } finally {
            // return rpc channel to reuse
            rpcChannelGroup.returnRpcChannel(rpcChannel);
        }
    }

    @Override
    public void init() {
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup);
        if (Epoll.isAvailable()) {
            bootstrap.channel(EpollSocketChannel.class);
            bootstrap.option(EpollChannelOption.EPOLL_MODE, EpollMode.EDGE_TRIGGERED);
            LOGGER.debug("NettyClient use Epoll Mode");
        } else {
            bootstrap.channel(NioSocketChannel.class);
            LOGGER.debug("NettyClient use Nio Mode");
        }

        bootstrap.option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE).option(ChannelOption.TCP_NODELAY, Boolean.TRUE)
            .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                getUri().getParameter(Constants.CONNECT_TIMEOUT_KEY, Constants.CONNECT_TIMEOUT_VALUE));

        final ChannelInitializer<SocketChannel> initializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new DecoderHandler()); // inbound 1 DecoderHandler
                if (getUri().getParameter(Constants.CONNECT_KEEPALIVE_ENABLED_KEY,
                    Constants.CONNECT_KEEPALIVE_ENABLED_VALUE)) {
                    ch.pipeline()
                        .addLast(new IdleStateHandler(
                            getUri().getParameter(Constants.READ_IDLE_TIMEOUT_KEY, Constants.READ_IDLE_TIMEOUT_VALUE),
                            0, 0, TimeUnit.SECONDS))
                        .addLast(new HeartbeatHandler()); // inbound 2 heartbeatHandler
                }

                ch.pipeline().addLast(new RpcHandler(NettyClient.this)) // inbound 3 RpcHandler
                    .addLast(new EncoderHandler()); // outbound EncoderHandler
            }
        };
        bootstrap.handler(initializer);
    }

    @Override
    public URI getUri() {
        return this.uri;
    }

    @Override
    public void close() {
        if (processor != null) {
            processor.close();
        }
        if (rpcChannelGroup != null) {
            rpcChannelGroup.close();
        }

        INSTANCE_SET.remove(this.uri.getAddress());
    }

    @Override
    public void setProcessor(Processor processor) {
        this.processor = processor;
    }

    @Override
    public Processor getProcessor() {
        return processor;
    }

    // 当前spi机制无法进行有参数的构造函数的扩展实现，替换方案为switch方式
    private RpcChannelGroup rpcChannelGroup(String channelType) {
        if (channelType != null) {
            switch (channelType) {
                case "long":
                    return new SingleRpcChannelGroup(getUri(), bootstrap);
                case "pool":
                    return new PooledRpcChannelGroup(getUri(), bootstrap);
                default:
                    throw new StarlightRpcException(
                        "RpcChannelGroup type {" + channelType + "} is illegal: not support.");
            }
        }
        throw new StarlightRpcException("RpcChannelGroup type is null");
    }

    @Override
    public PeerStatus status() {
        return this.status;
    }

    @Override
    public void gracefullyShutdown(long quietPeriod, long timeout) {

        try {
            updateStatus(new PeerStatus(PeerStatus.Status.SHUTTING_DOWN, System.currentTimeMillis()));

            // wait for all request has been done
            if (timeout > 0) {
                long shutdownTimeoutTime = System.currentTimeMillis() + timeout * 1000;
                for (;;) {
                    int unfinishedCallbackNum = 0;
                    for (RpcChannel rpcChannel : rpcChannelGroup.allRpcChannels()) {
                        if (rpcChannel != null) {
                            unfinishedCallbackNum = unfinishedCallbackNum + rpcChannel.allCallbacks().size();
                        }
                    }

                    if (getProcessor().allWaitTaskCount().equals(0) && unfinishedCallbackNum == 0) {
                        LOGGER.info("NettyClient has processed all requests, shutdown. RemoteAddr {}",
                            getUri().getAddress());
                        break;
                    }

                    if (System.currentTimeMillis() >= shutdownTimeoutTime) {
                        LOGGER.info(
                            "NettyClient reach the maximum timeout time, force shutdown. RemoteAddr {}."
                                + "Number of unfinished task {}, Number of unfinished request {}. "
                                + "Will response timeout",
                            getUri().getAddress(), getProcessor().allWaitTaskCount(), unfinishedCallbackNum);
                        for (RpcChannel rpcChannel : rpcChannelGroup.allRpcChannels()) {
                            if (rpcChannel.allCallbacks() != null && rpcChannel.allCallbacks().size() > 0) {
                                for (RpcCallback rpcCallback : rpcChannel.allCallbacks().values()) {
                                    rpcCallback.onError(StarlightRpcException.timeoutException(rpcCallback.getRequest(),
                                        getUri().getAddress()));
                                }
                                rpcChannel.allCallbacks().clear();
                            }
                        }
                        break;
                    }

                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
            }

            close();

            updateStatus(new PeerStatus(PeerStatus.Status.SHUTDOWN, System.currentTimeMillis()));
        } catch (Exception e) {
            LOGGER.error("An exception occur when NettyClient shutdownGracefully.", e);
        }
    }

    @Override
    public synchronized void updateStatus(PeerStatus newStatus) {
        if (status == null) {
            LOGGER.debug("Update {} status from {} to {}", getUri().getAddress(), status, newStatus);
            status = newStatus;
            return;
        }

        // FIXME 状态更新时是否要考虑并发的问题
        if (PeerStatus.Status.SHUTTING_DOWN.equals(status.getStatus())
            && PeerStatus.Status.OUTLIER.equals(newStatus.getStatus())) {
            LOGGER.warn("Forbidden to change status of ClientPeer {} from SHUTTINGDOWN to OUTLIER",
                getUri().getAddress());
            return;
        }

        if (PeerStatus.Status.SHUTDOWN.equals(status.getStatus())
            && PeerStatus.Status.OUTLIER.equals(newStatus.getStatus())) {
            LOGGER.warn("Forbidden to change status of ClientPeer {} from SHUTDOWN to OUTLIER", getUri().getAddress());
            return;
        }

        if (PeerStatus.Status.OUTLIER.equals(newStatus.getStatus())
            && PeerStatus.Status.OUTLIER.equals(status.getStatus())) {
            LOGGER.warn("Forbidden to change status of ClientPeer {} from OUTLIER {} to OUTLIER {}",
                getUri().getAddress(), status.getStatusRecordTime(), newStatus.getStatusRecordTime());
            return;
        }

        LOGGER.debug("Update {} status from {} to {}", getUri().getAddress(), status, newStatus);
        this.status = newStatus;
    }
}
