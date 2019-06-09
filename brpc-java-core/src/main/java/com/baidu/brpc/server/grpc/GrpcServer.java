package com.baidu.brpc.server.grpc;

import com.baidu.brpc.naming.*;
import com.baidu.brpc.protocol.Options;
import com.baidu.brpc.server.RpcServerOptions;
import com.baidu.brpc.server.ServiceManager;
import com.baidu.brpc.spi.ExtensionLoaderManager;
import com.baidu.brpc.thread.ShutDownManager;
import com.baidu.brpc.utils.CustomThreadFactory;
import com.baidu.brpc.utils.NetUtils;
import com.baidu.brpc.utils.ThreadPool;
import io.grpc.*;
import io.grpc.internal.ServerListener;
import io.grpc.netty.NettyServerBuilder;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollMode;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Grpc support in brpc
 * This implementation is an warpper of grpc in brpc,
 */
@Getter
public class GrpcServer {


    private static final Logger LOG = LoggerFactory.getLogger(GrpcServer.class);

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
     * grpc server
     */
    private Server grpcServer;

    /**
     * netty io thread pool
     */
    private EventLoopGroup bossGroup;
    // netty io thread pool
    private EventLoopGroup workerGroup;
    private List<ServerInterceptor> interceptors = new ArrayList<ServerInterceptor>();
    private ThreadPool threadPool;
    private List<ThreadPool> customThreadPools = new ArrayList<ThreadPool>();
    private NamingService namingService;
    private List<Object> serviceList = new ArrayList<Object>();
    private List<RegisterInfo> registerInfoList = new ArrayList<RegisterInfo>();
    // private ServerStatus serverStatus;
    private AtomicBoolean stop = new AtomicBoolean(true);


    private NettyServerBuilder nettyServerBuilder;


    public GrpcServer(int port) {
        this(null, port);
    }

    public GrpcServer(int port, RpcServerOptions options) {
        this(null, port, options, null);
    }

    public GrpcServer(String host, int port) {
        this(host, port, new RpcServerOptions());
    }

    public GrpcServer(String host, int port, RpcServerOptions options) {
        this(host, port, options, new ArrayList<ServerInterceptor>());
    }

    public GrpcServer(String host, int port, RpcServerOptions options, List<ServerInterceptor> interceptors) {
        this.host = host;
        this.port = port;
        if (options != null) {
            try {
                this.rpcServerOptions.copyFrom(options);
            } catch (Exception ex) {
                LOG.warn("init options failed, so use default");
            }
        }

        // check protocol setting
        if (rpcServerOptions.getProtocolType() != null) {
            if (rpcServerOptions.getProtocolType() != Options.ProtocolType.PROTOCOL_GRPC_VALUE) {
                LOG.error("You are using rpcServerOptions, Please set ProtocolType to PROTOCOL_GRPC_VALUE in order to avoid confuse");
                System.exit(-1);
            }

        }

        if (interceptors != null) {
            this.interceptors.addAll(interceptors);
        }

        // 判断是否在jarvis环境，若是jarvis环境则以环境变量port为准，否则以用户自定义的port为准
        if (rpcServerOptions.getJarvisPortName() != null) {
            if (System.getenv(rpcServerOptions.getJarvisPortName()) != null) {
                this.port = Integer.valueOf(System.getenv(rpcServerOptions.getJarvisPortName()));
            }
        }

        // Init NettyServerBuilder
        if (host == null) {
            nettyServerBuilder = NettyServerBuilder.forPort(port);
        } else {
            nettyServerBuilder = NettyServerBuilder.forAddress(new InetSocketAddress(host, port));
        }


        // Init NamingService
        ExtensionLoaderManager.getInstance().loadAllExtensions(rpcServerOptions.getEncoding());
        if (StringUtils.isNotBlank(rpcServerOptions.getNamingServiceUrl())) {
            BrpcURL url = new BrpcURL(rpcServerOptions.getNamingServiceUrl());
            NamingServiceFactory namingServiceFactory = NamingServiceFactoryManager.getInstance()
                    .getNamingServiceFactory(url.getSchema());
            this.namingService = namingServiceFactory.createNamingService(url);
        }

        if (Epoll.isAvailable()) {
            bossGroup = new EpollEventLoopGroup(rpcServerOptions.getAcceptorThreadNum(),
                    new CustomThreadFactory("server-acceptor-thread"));
            workerGroup = new EpollEventLoopGroup(rpcServerOptions.getIoThreadNum(),
                    new CustomThreadFactory("server-io-thread"));
            ((EpollEventLoopGroup) bossGroup).setIoRatio(100);
            ((EpollEventLoopGroup) workerGroup).setIoRatio(100);
            nettyServerBuilder.bossEventLoopGroup(bossGroup);
            nettyServerBuilder.workerEventLoopGroup(workerGroup);
            nettyServerBuilder.withChildOption(EpollChannelOption.EPOLL_MODE, EpollMode.EDGE_TRIGGERED);
            LOG.info("use epoll edge trigger mode");
        } else {
            bossGroup = new NioEventLoopGroup(rpcServerOptions.getAcceptorThreadNum(),
                    new CustomThreadFactory("server-acceptor-thread"));
            workerGroup = new NioEventLoopGroup(rpcServerOptions.getIoThreadNum(),
                    new CustomThreadFactory("server-io-thread"));
            ((NioEventLoopGroup) bossGroup).setIoRatio(100);
            ((NioEventLoopGroup) workerGroup).setIoRatio(100);

            nettyServerBuilder.bossEventLoopGroup(bossGroup);
            nettyServerBuilder.workerEventLoopGroup(workerGroup);
            LOG.info("use normal mode");
        }

        /**
         * The setting "SO_BACKLOG" for BossGroup is 128,
         * You should not set the following value unless you know what are you doing
         *
         * @see io.grpc.netty.NettyServer#start(ServerListener)
         */
        nettyServerBuilder.withChildOption(ChannelOption.SO_KEEPALIVE, rpcServerOptions.isKeepAlive());
        nettyServerBuilder.withChildOption(ChannelOption.TCP_NODELAY, rpcServerOptions.isTcpNoDelay());
        nettyServerBuilder.withChildOption(ChannelOption.SO_REUSEADDR, true);
        nettyServerBuilder.withChildOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        nettyServerBuilder.withChildOption(ChannelOption.SO_LINGER, rpcServerOptions.getSoLinger());
        nettyServerBuilder.withChildOption(ChannelOption.SO_SNDBUF, rpcServerOptions.getSendBufferSize());
        nettyServerBuilder.withChildOption(ChannelOption.SO_RCVBUF, rpcServerOptions.getReceiveBufferSize());

        // register shutdown hook to jvm
        ShutDownManager.getInstance();
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                GrpcServer.this.shutdown();
            }
        }));


    }

    public void registerService(Object service) {
        registerService(service, null, rpcServerOptions);
    }

    public void registerService(Object service, NamingOptions namingOptions, RpcServerOptions serverOptions) {
        if (service instanceof BindableService || service instanceof ServerServiceDefinition) {
            ServerServiceDefinition serviceDefinition;
            if(service instanceof BindableService){
                BindableService bindableService = (BindableService) service;
                serviceDefinition =  ServerInterceptors.interceptForward(bindableService, this.interceptors);

            } else {
                serviceDefinition =  ServerInterceptors.interceptForward((ServerServiceDefinition)service, this.interceptors);
            }

            // grpc context will take over the lifecycle of services
            nettyServerBuilder.addService(serviceDefinition);

            RegisterInfo registerInfo = new RegisterInfo();
            String interfaceName = serviceDefinition.getServiceDescriptor().getName();
            registerInfo.setInterfaceName(interfaceName);
            registerInfo.setHost(NetUtils.getLocalAddress().getHostAddress());
            registerInfo.setPort(port);
            if (namingOptions != null) {
                registerInfo.setGroup(namingOptions.getGroup());
                registerInfo.setVersion(namingOptions.getVersion());
                registerInfo.setIgnoreFailOfNamingService(namingOptions.isIgnoreFailOfNamingService());
            }
            registerInfoList.add(registerInfo);
        }
    }


    public void start() {

        if (nettyServerBuilder != null) {
            this.grpcServer = nettyServerBuilder.build();
            try {
                if (stop.compareAndSet(true, false)) {
                    grpcServer.start();

                    //regist all service to naming service, for service discovery and load balance
                    if (namingService != null) {
                        for (RegisterInfo registerInfo : registerInfoList) {
                            namingService.register(registerInfo);
                        }
                    }

                    grpcServer.awaitTermination();
                }
            } catch (IOException e) {
                stop.compareAndSet(false, true);
                e.printStackTrace();
            } catch (InterruptedException e) {
                stop.compareAndSet(false, true);
                e.printStackTrace();
            }
        }

    }

    public void shutdown() {

        if (stop.compareAndSet(false, true)) {
            if (namingService != null) {
                for (RegisterInfo registerInfo : registerInfoList) {
                    namingService.unregister(registerInfo);
                }
            }
            if (grpcServer != null) {
                grpcServer.shutdown();
            }
        }

    }
}
