package com.baidu.brpc.server;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.protocol.standard.Echo;
import com.baidu.brpc.protocol.standard.Echo.EchoRequest;
import com.baidu.brpc.protocol.standard.EchoService;
import com.baidu.brpc.protocol.standard.EchoServiceImpl;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerInitTest {

    @Test
    public void testInitServerMultiTimes() throws Exception {

        RpcServer rpcServer1 = new RpcServer(8000);
        rpcServer1.registerService(new EchoServiceImpl());
        rpcServer1.start();

        RpcServer rpcServer2 = new RpcServer(8001);
        RpcServerOptions options = new RpcServerOptions();
        rpcServer2.registerService(new EchoServiceImpl(), options);
        rpcServer2.start();

        RpcClient secondRpcClient = new RpcClient("list://127.0.0.1:8001");
        EchoService echoService = BrpcProxy.getProxy(secondRpcClient, EchoService.class);
        EchoRequest request = Echo.EchoRequest.newBuilder().setMessage("hello").build();
        echoService.echo(request);

        int processor = Runtime.getRuntime().availableProcessors();
        ThreadNumStat stat1 = calThreadNum();
        Assert.assertEquals(processor, stat1.ioThreadNum);
        Assert.assertEquals(processor, stat1.workThreadNum);
        Assert.assertEquals(processor, stat1.customWorkThreadNum);

        rpcServer1.shutdown();
        rpcServer2.shutdown();
        Thread.sleep(5);
        
        ThreadNumStat stat2 = calThreadNum();
        Assert.assertEquals(processor, stat2.ioThreadNum);
        Assert.assertEquals(processor, stat2.workThreadNum);
        Assert.assertEquals(0, stat2.customWorkThreadNum);

    }

    private ThreadNumStat calThreadNum() {
        ThreadNumStat stat = new ThreadNumStat();

        // stat all thread
        Map<Thread, StackTraceElement[]> allThreadMap = Thread.getAllStackTraces();
        for (Map.Entry<Thread, StackTraceElement[]> entry : allThreadMap.entrySet()) {

            Thread thread = entry.getKey();

            if (thread.getName().contains("brpc-io-thread")) {
                stat.ioThreadNum ++;
            } else if (thread.getName().contains("brpc-work-thread")) {
                stat.workThreadNum ++;
            } else if (thread.getName().contains("server-acceptor-thread")) {
                stat.acceptorThreadNum ++;
            } else if (thread.getName().contains("EchoServiceImpl-work-thread")) {
                stat.customWorkThreadNum ++;
            }
        }

        log.info("thread statistic data, ioThreadNum : {}, \n workThreadNum : {}, acceptorThreadNum : {}, "
                        + "customizedWorkThreadNum : {}",
                stat.ioThreadNum, stat.workThreadNum, stat.acceptorThreadNum, stat.customWorkThreadNum);

        return stat;
    }

    public static class ThreadNumStat {
        public int ioThreadNum;
        public int workThreadNum;
        public int acceptorThreadNum;
        public int customWorkThreadNum;
    }

}
