package com.baidu.brpc.thread;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.baidu.brpc.RpcOptionsUtils;
import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.protocol.Options;
import com.baidu.brpc.protocol.standard.Echo;
import com.baidu.brpc.protocol.standard.EchoService;
import com.baidu.brpc.protocol.standard.EchoServiceImpl;
import com.baidu.brpc.push.userservice.UserPushApiImpl;
import com.baidu.brpc.server.RpcServer;
import com.baidu.brpc.server.RpcServerOptions;
import com.baidu.brpc.server.ServiceManager;
import com.baidu.brpc.utils.ThreadPool;

import io.netty.channel.EventLoopGroup;

public class GlobalThreadPoolSharingTest {

    @Before
    public void init() {
        if (ServiceManager.getInstance() != null) {
            ServiceManager.getInstance().getServiceMap().clear();
        }
    }

    @Test
    @Ignore
    public void testShareGlobalThreadPool() {
        RpcServerOptions rpcServerOptions = RpcOptionsUtils.getRpcServerOptions();
        rpcServerOptions.setProtocolType(Options.ProtocolType.PROTOCOL_SERVER_PUSH_VALUE);
        rpcServerOptions.setGlobalThreadPoolSharing(true);
        RpcServer rpcServer1 = new RpcServer(8000, rpcServerOptions);
        rpcServer1.registerService(new EchoServiceImpl());
        rpcServer1.start();
        RpcServer rpcServer2 = new RpcServer(8001, rpcServerOptions);
        rpcServer2.registerService(new EchoServiceImpl());
        rpcServer2.start();
        Assert.assertTrue(rpcServer1.getBossGroup() == rpcServer2.getBossGroup());
        Assert.assertTrue(rpcServer1.getWorkerGroup() == rpcServer2.getWorkerGroup());
        Assert.assertTrue(rpcServer1.getThreadPool() == rpcServer2.getThreadPool());

        RpcClientOptions rpcClientOptions = RpcOptionsUtils.getRpcClientOptions();
        rpcClientOptions.setProtocolType(Options.ProtocolType.PROTOCOL_SERVER_PUSH_VALUE);
        rpcClientOptions.setClientName("c1");
        rpcClientOptions.setGlobalThreadPoolSharing(true);

        RpcClient rpcClient1 = new RpcClient("list://127.0.0.1:8000", rpcClientOptions);
        EchoService echoService1 = BrpcProxy.getProxy(rpcClient1, EchoService.class);
        rpcClient1.registerPushService(new UserPushApiImpl());
        RpcClient rpcClient2 = new RpcClient("list://127.0.0.1:8001", rpcClientOptions);
        EchoService echoService2 = BrpcProxy.getProxy(rpcClient2, EchoService.class);
        rpcClient2.registerPushService(new UserPushApiImpl());
        RpcClient rpcClient3 = new RpcClient("list://127.0.0.1:8001", rpcClientOptions);
        EchoService echoService3 = BrpcProxy.getProxy(rpcClient3, EchoService.class);
        rpcClient3.registerPushService(new UserPushApiImpl());
        BrpcThreadPoolManager threadPoolManager = BrpcThreadPoolManager.getInstance();
        Assert.assertTrue(threadPoolManager.getIoThreadPoolMap().size() == 1);
        Assert.assertTrue(threadPoolManager.getWorkThreadPoolMap().size() == 1);

        Echo.EchoRequest request = Echo.EchoRequest.newBuilder().setMessage("hello").build();
        for (int i = 0; i < 1000; i++) {
            Echo.EchoResponse response = echoService1.echo(request);
            Echo.EchoResponse response2 = echoService2.echo(request);
            Echo.EchoResponse response3 = echoService3.echo(request);
            assertEquals("hello", response.getMessage());
            assertEquals("hello", response2.getMessage());
            assertEquals("hello", response3.getMessage());
        }

        // test shutndown and stop

        rpcClient1.stop();
        rpcClient2.stop();
        rpcClient3.stop();
        rpcServer1.shutdown();
        rpcServer2.shutdown();
        // client
        Assert.assertTrue(threadPoolManager.getIoThreadPoolMap().size() == 1);
        Assert.assertTrue(threadPoolManager.getWorkThreadPoolMap().size() == 1);

        // server
        EventLoopGroup r1BossGroup = rpcServer1.getBossGroup();
        EventLoopGroup r1WorkerGroup = rpcServer1.getWorkerGroup();
        ThreadPool r1ThreadPool = rpcServer1.getThreadPool();

        Assert.assertFalse(r1BossGroup.isShutdown());
        Assert.assertFalse(r1WorkerGroup.isShutdown());
        Assert.assertFalse(r1ThreadPool.isStopped());

        ShutDownManager.shutdownGlobalThreadPools();

        try {
            Thread.sleep(5 * 1000L);
        } catch (InterruptedException e) {
            // do nothing
        }

        Assert.assertTrue(threadPoolManager.getIoThreadPoolMap().size() == 0);
        Assert.assertTrue(threadPoolManager.getWorkThreadPoolMap().size() == 0);

        Assert.assertTrue(r1BossGroup.isShutdown());
        Assert.assertTrue(r1WorkerGroup.isShutdown());
        Assert.assertTrue(r1ThreadPool.isStopped());

    }
}
