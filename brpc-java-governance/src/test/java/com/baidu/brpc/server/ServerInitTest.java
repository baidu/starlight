package com.baidu.brpc.server;

import java.util.Map;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.protocol.standard.Echo;
import com.baidu.brpc.protocol.standard.Echo.EchoRequest;
import com.baidu.brpc.protocol.standard.EchoService;
import com.baidu.brpc.protocol.standard.EchoServiceImpl;
import com.baidu.brpc.RpcOptionsUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerInitTest {

    @Test
    @Ignore("Fix this test later")
    public void testInitServerMultiTimes() throws Exception {

        RpcServer rpcServer1 = new RpcServer(8000, RpcOptionsUtils.getRpcServerOptions());
        rpcServer1.registerService(new EchoServiceImpl());
        rpcServer1.start();

        RpcServer rpcServer2 = new RpcServer(8001, RpcOptionsUtils.getRpcServerOptions());
        rpcServer2.registerService(new EchoServiceImpl(), RpcOptionsUtils.getRpcServerOptions());
        rpcServer2.start();

        RpcClient secondRpcClient = new RpcClient("list://127.0.0.1:8001",
                RpcOptionsUtils.getRpcClientOptions());
        EchoService echoService = BrpcProxy.getProxy(secondRpcClient, EchoService.class);
        EchoRequest request = Echo.EchoRequest.newBuilder().setMessage("hello").build();
        echoService.echo(request);

        int processor = Runtime.getRuntime().availableProcessors();
        ThreadNumStat stat1 = calThreadNum();
        Assert.assertEquals(2, stat1.serverWorkThreadNum);
        Assert.assertEquals(1, stat1.customWorkThreadNum);
        Assert.assertEquals(1, stat1.clientWorkThreadNum);

        rpcServer1.shutdown();
        rpcServer2.shutdown();
        Thread.sleep(3);

        ThreadNumStat stat2 = calThreadNum();
        Assert.assertEquals(0, stat2.serverIoThreadNum);
        Assert.assertEquals(0, stat2.serverWorkThreadNum);
        Assert.assertEquals(0, stat2.customWorkThreadNum);
        secondRpcClient.shutdown();
    }

    private ThreadNumStat calThreadNum() {
        ThreadNumStat stat = new ThreadNumStat();

        // stat all thread
        Map<Thread, StackTraceElement[]> allThreadMap = Thread.getAllStackTraces();
        for (Map.Entry<Thread, StackTraceElement[]> entry : allThreadMap.entrySet()) {

            Thread thread = entry.getKey();

            if (thread.getName().contains("server-io-thread")) {
                stat.serverIoThreadNum++;
            } else if (thread.getName().contains("server-work-thread")) {
                stat.serverWorkThreadNum++;
            } else if (thread.getName().contains("client-io-thread")) {
                stat.clientIoThreadNum++;
            } else if (thread.getName().contains("client-work-thread")) {
                stat.clientWorkThreadNum++;
            } else if (thread.getName().contains("server-acceptor-thread")) {
                stat.acceptorThreadNum++;
            } else if (thread.getName().contains("EchoServiceImpl-work-thread")) {
                stat.customWorkThreadNum++;
            }
        }

        log.info("thread statistic data, serverIoThreadNum : {}, serverWorkThreadNum : {}, "
                        + "clientIoThreadNum : {}, clientWorkThreadNum : {}, acceptorThreadNum : {}, "
                        + "customizedWorkThreadNum : {}",
                stat.serverIoThreadNum, stat.serverWorkThreadNum,
                stat.clientIoThreadNum, stat.clientWorkThreadNum,
                stat.acceptorThreadNum, stat.customWorkThreadNum);

        return stat;
    }

    public static class ThreadNumStat {
        public int serverIoThreadNum;
        public int serverWorkThreadNum;
        public int clientIoThreadNum;
        public int clientWorkThreadNum;
        public int acceptorThreadNum;
        public int customWorkThreadNum;
    }


}
