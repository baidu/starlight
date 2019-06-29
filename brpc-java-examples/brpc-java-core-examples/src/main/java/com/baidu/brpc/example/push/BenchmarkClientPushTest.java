package com.baidu.brpc.example.push;

import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcCallback;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.client.loadbalance.LoadBalanceStrategy;
import com.baidu.brpc.example.interceptor.CustomInterceptor;
import com.baidu.brpc.example.push.normal.EchoRequest;
import com.baidu.brpc.example.push.normal.EchoResponse;
import com.baidu.brpc.example.push.normal.EchoServiceAsync;
import com.baidu.brpc.example.push.push.UserPushApiImpl;
import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.protocol.Options;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class BenchmarkClientPushTest {

    public static class SendInfo {
        public long successRequestNum = 0;
        public long failRequestNum = 0;
        public long elapsedNs = 0;
    }

    private static volatile boolean stop = false;

    // rpc press: send pressure to RpcServerTest
    // push & rpc press: send pressure to BenchmarkServerPushTest

    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println("usage: server_ip:server_port threadNum");
            System.exit(-1);
        }

        RpcClientOptions clientOption = new RpcClientOptions();
        clientOption.setProtocolType(Options.ProtocolType.PROTOCOL_SERVER_PUSH_VALUE);
        clientOption.setWriteTimeoutMillis(1000);
        clientOption.setReadTimeoutMillis(1000);
        clientOption.setMaxTotalConnections(1000);
//        clientOption.setMaxTotalConnections(10);
        clientOption.setMinIdleConnections(100);
        clientOption.setLoadBalanceType(LoadBalanceStrategy.LOAD_BALANCE_FAIR);
        clientOption.setCompressType(Options.CompressType.COMPRESS_TYPE_NONE);
        clientOption.setClientName("Benchmark");

        int threadNum = Integer.parseInt(args[1]);

        String serviceUrl = args[0];
//        String serviceUrl = "list://127.0.0.1:8012";

        List<Interceptor> interceptors = new ArrayList<Interceptor>();
        interceptors.add(new CustomInterceptor());

        RpcClient rpcClient = new RpcClient(serviceUrl, clientOption);
        EchoServiceAsync echoService = BrpcProxy.getProxy(rpcClient, EchoServiceAsync.class);
        rpcClient.registerPushService(new UserPushApiImpl());

        byte[] messageBytes = null;
        try {
            InputStream inputStream = Thread.currentThread().getClass()
                    .getResourceAsStream("/message_1k.txt");

            int length = inputStream.available();
            messageBytes = new byte[length];
            inputStream.read(messageBytes);
        } catch (IOException ex) {
            System.exit(1);
        }

        SendInfo[] sendInfos = new SendInfo[threadNum];
        Thread[] threads = new Thread[threadNum];

        for (int i = 0; i < threadNum; i++) {
            sendInfos[i] = new SendInfo();
            threads[i] = new Thread(new ThreadTask(messageBytes, sendInfos[i], echoService), "Benchnark-" + i);
            threads[i].start();
        }

        long lastSuccessRequestNum = 0;
        long lastFailRequestNum = 0;
        long lastElapsedNs = 0;
        int second = 0;
        long skippedQps = 0;
        while (!stop) {
            long beginTime = System.nanoTime();
            try {
                Thread.sleep(1000);
                ++second;

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }

            long successNum = 0;
            long failNum = 0;
            long elapseNs = 0;
            long averageElapsedNs = 0;
            for (SendInfo sendInfo : sendInfos) {
                successNum += sendInfo.successRequestNum;
                failNum += sendInfo.failRequestNum;
                elapseNs += sendInfo.elapsedNs;
            }
            if (successNum - lastSuccessRequestNum > 0) {
                averageElapsedNs = (elapseNs - lastElapsedNs) / (successNum - lastSuccessRequestNum);
            }
            long endTime = System.nanoTime();

            String msg = String.format("success=%s,fail=%s,average=%sns",
                    (successNum - lastSuccessRequestNum) * 1000 * 1000 * 1000 / (endTime - beginTime),
                    (failNum - lastFailRequestNum) * 1000 * 1000 * 1000 / (endTime - beginTime),
                    averageElapsedNs);

            lastSuccessRequestNum = successNum;
            lastFailRequestNum = failNum;
            lastElapsedNs = elapseNs;

            // 从第10开始计算平均qps
            if (second > 30) {
                long avgQps = (lastSuccessRequestNum - skippedQps) / (second - 30);
                msg = msg + ", avgQps=" + avgQps;
            } else {
                skippedQps = lastSuccessRequestNum;
            }

            log.info(msg);

        }
    }

    public static class EchoCallback implements RpcCallback<EchoResponse> {
        private long startTime;
        private SendInfo sendInfo;

        public EchoCallback(long startTime, SendInfo sendInfo) {
            this.startTime = startTime;
            this.sendInfo = sendInfo;
        }

        @Override
        public void success(EchoResponse response) {
            if (response != null) {
                sendInfo.successRequestNum++;
                long elapseTimeNs = System.nanoTime() - startTime;
                sendInfo.elapsedNs += elapseTimeNs;
                log.debug("client async call success, elapseTimeNs={}, response={}",
                        System.nanoTime() - startTime, response.getMessage());
            } else {
                sendInfo.failRequestNum++;
                log.debug("client async call failed");
            }
        }

        @Override
        public void fail(Throwable e) {
            sendInfo.failRequestNum++;
            log.debug("client async call failed, {}", e.getMessage());
        }
    }

    public static class ThreadTask implements Runnable {
        private byte[] messageBytes;
        private SendInfo sendInfo;
        private EchoServiceAsync echoService;

        public ThreadTask(byte[] messageBytes,
                          SendInfo sendInfo, EchoServiceAsync echoService) {
            this.messageBytes = messageBytes;
            this.sendInfo = sendInfo;
            this.echoService = echoService;
        }

        public void run() {
            // build request
            EchoRequest request = new EchoRequest();
            request.setMessage(new String(messageBytes));
            while (!stop) {
                try {
                    echoService.echo(request, new EchoCallback(System.nanoTime(), sendInfo));
                } catch (Exception ex) {
                    log.info("client send exception:", ex);
                    sendInfo.failRequestNum++;
                }
            }
        }
    }

}
