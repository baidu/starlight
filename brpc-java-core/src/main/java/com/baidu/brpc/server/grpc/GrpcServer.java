package com.baidu.brpc.server.grpc;

import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.naming.NamingOptions;
import com.baidu.brpc.naming.NamingService;
import com.baidu.brpc.naming.RegisterInfo;
import com.baidu.brpc.server.RpcServer;
import com.baidu.brpc.server.RpcServerOptions;
import com.baidu.brpc.server.ServerStatus;
import com.baidu.brpc.utils.ThreadPool;
import com.sun.xml.internal.ws.api.client.ServiceInterceptor;
import io.grpc.*;
import io.grpc.netty.NettyServerBuilder;
import io.netty.channel.EventLoopGroup;
import lombok.Getter;
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
    private NamingService namingService;
    private List<Object> serviceList = new ArrayList<Object>();
    private List<RegisterInfo> registerInfoList = new ArrayList<RegisterInfo>();
    private ServerStatus serverStatus;
    private AtomicBoolean stop = new AtomicBoolean(false);


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
        if (interceptors != null) {
            this.interceptors.addAll(interceptors);
        }
        if (nettyServerBuilder == null) {
            if (host == null) {
                nettyServerBuilder = NettyServerBuilder.forPort(port);
            } else {
                nettyServerBuilder = NettyServerBuilder.forAddress(new InetSocketAddress(host, port));
            }

        }

        //TODO Initialize naming service for service registration

    }

    public void registerService(Object service) {
        if (service instanceof BindableService) {
            BindableService bindableService = (BindableService) service;
            ServerServiceDefinition serviceDefinition = ServerInterceptors.interceptForward(bindableService, this.interceptors);

            //TODO Get service definition from ServerServiceDefinition, in order to initialize RegisterInfo

            nettyServerBuilder.addService(serviceDefinition);
        }
    }

    public void start() {

        if (nettyServerBuilder != null) {
            this.grpcServer = nettyServerBuilder.build();
            try {
                grpcServer.start();
                stop.set(false);
                grpcServer.awaitTermination();
            } catch (IOException e) {
                stop.set(true);
                e.printStackTrace();
            } catch (InterruptedException e) {
                stop.set(true);
                e.printStackTrace();
            }
        }

    }

    public void shutdown() {
        if (grpcServer != null) {
            grpcServer.shutdown();
            stop.set(true);
        }
    }
}
