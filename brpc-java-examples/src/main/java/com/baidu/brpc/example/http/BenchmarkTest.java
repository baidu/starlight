package com.baidu.brpc.example.http;

import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcCallback;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.client.loadbalance.LoadBalanceStrategy;
import com.baidu.brpc.client.loadbalance.LoadBalanceType;
import com.baidu.brpc.example.standard.Echo;
import com.baidu.brpc.example.standard.Echo.EchoRequest;
import com.baidu.brpc.example.standard.Echo.EchoResponse;
import com.baidu.brpc.example.standard.EchoServiceAsync;
import com.baidu.brpc.protocol.Options.ProtocolType;

public class BenchmarkTest {

    private static volatile int successRequestNum = 0;
    private static volatile int failRequestNum = 0;
    private static volatile long totalElapsedNs = 0;
    private static volatile boolean stop = false;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("usage: BenchmarkTest list://127.0.0.1:8080 threadNum");
            System.exit(-1);
        }
        RpcClientOptions options = new RpcClientOptions();
        options.setProtocolType(ProtocolType.PROTOCOL_HTTP_PROTOBUF_VALUE);
        options.setLoadBalanceType(LoadBalanceStrategy.LOAD_BALANCE_FAIR);
        options.setMaxTotalConnections(1000000);
        options.setMinIdleConnections(10);
        options.setReadTimeoutMillis(5000);
        options.setConnectTimeoutMillis(1000);
        RpcClient rpcClient = new RpcClient(args[0], options, null);
        EchoServiceAsync echoServiceAsync = BrpcProxy.getProxy(rpcClient, EchoServiceAsync.class);
        int threadNum = Integer.parseInt(args[1]);
        Thread[] threads = new Thread[threadNum];
        for (int i = 0; i < threadNum; i++) {
            threads[i] = new Thread(new ThreadTask(echoServiceAsync), "work-thread-" + i);
            threads[i].start();
        }
        Thread qpsThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!stop) {
                    int lastSuccessRequestNum = successRequestNum;
                    int lastFailRequestNum = failRequestNum;
                    long lastElapsedNs = totalElapsedNs;
                    try {
                        Thread.sleep(1000);
                    } catch (Exception ex) {
                        System.out.println(ex.getMessage());
                    }
                    int successNum = successRequestNum - lastSuccessRequestNum;
                    int failNum = failRequestNum - lastFailRequestNum;
                    long averageElapsedNs = 0;
                    if (successNum > 0) {
                        averageElapsedNs = (totalElapsedNs - lastElapsedNs) / successNum;
                    }
                    System.out.printf("success=%d,fail=%d,average=%dns\n",
                            successNum, failNum, averageElapsedNs);
                }
            }
        }, "stat-qps-thread");
        qpsThread.start();

        for (int i = 0; i < threadNum; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException ex) {
                // ignore
            }

        }
    }

    public static class EchoCallback implements RpcCallback<EchoResponse> {
        private long startTime;

        public EchoCallback(long startTime) {
            this.startTime = startTime;
        }

        @Override
        public void success(EchoResponse response) {
            if (response != null) {
                successRequestNum++;
                long elapseTimeNs = System.nanoTime() - startTime;
                totalElapsedNs += elapseTimeNs;
//                System.out.printf("async call success, elapseTimeNs=%d, response=%s\n",
//                        System.nanoTime() - startTime, response.getMessage());
            } else {
                failRequestNum++;
//                System.out.println("async call failed");
            }
        }

        @Override
        public void fail(Throwable e) {
            failRequestNum++;
//            System.out.printf("async call failed, %s\n", e.getMessage());
        }
    }

    public static class ThreadTask implements Runnable {

        private EchoServiceAsync echoServiceAsync;

        public ThreadTask(EchoServiceAsync echoServiceAsync) {
            this.echoServiceAsync = echoServiceAsync;
        }

        public void run() {
            while (!stop) {
                try {
                    // build request
                    EchoRequest request = Echo.EchoRequest.newBuilder().setMessage("hello").build();
                    echoServiceAsync.echo(request, new EchoCallback(System.nanoTime()));
                } catch (Exception ex) {
                    System.out.println("send exception:" + ex.getMessage());
                }
            }
        }

    }
}
