/*
 * Copyright (c) 2018 Baidu, Inc. All Rights Reserved.
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

package com.baidu.brpc.example.standard;

import java.io.IOException;
import java.io.InputStream;

import com.baidu.brpc.RpcContext;
import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcCallback;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.client.channel.ChannelType;
import com.baidu.brpc.client.loadbalance.LoadBalanceStrategy;
import com.baidu.brpc.protocol.Options;

import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by wenweihu86 on 2017/5/1.
 */
@Slf4j
public class BenchmarkTest {
    public static class SendInfo {
        public long successRequestNum = 0;
        public long failRequestNum = 0;
        public long elapsedNs = 0;
    }

    private static volatile boolean stop = false;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("usage: BenchmarkTest 127.0.0.1:8002 threadNum");
            System.exit(-1);
        }
        RpcClientOptions options = new RpcClientOptions();
        options.setProtocolType(Options.ProtocolType.PROTOCOL_BAIDU_STD_VALUE);
        options.setLoadBalanceType(LoadBalanceStrategy.LOAD_BALANCE_FAIR);
        options.setMaxTotalConnections(1000000);
        options.setMinIdleConnections(10);
        options.setConnectTimeoutMillis(1000);
        options.setWriteTimeoutMillis(1000);
        options.setReadTimeoutMillis(1000);
        options.setTcpNoDelay(false);
        options.setChannelType(ChannelType.SINGLE_CONNECTION);
//        options.setWorkThreadNum(2);
        // options.setFutureBufferSize(10000);
        RpcClient rpcClient = new RpcClient(args[0], options);
        int threadNum = Integer.parseInt(args[1]);

        byte[] messageBytes = null;
        try {
            InputStream inputStream = Thread.currentThread().getClass()
                    .getResourceAsStream("/message_1k.txt");
            int length = inputStream.available();
            messageBytes = new byte[length];
            inputStream.read(messageBytes);
            log.info("message size=" + messageBytes.length);
        } catch (IOException ex) {
            System.exit(1);
        }

        EchoServiceAsync echoService = BrpcProxy.getProxy(rpcClient, EchoServiceAsync.class);

        SendInfo[] sendInfos = new SendInfo[threadNum];
        Thread[] threads = new Thread[threadNum];
        for (int i = 0; i < threadNum; i++) {
            sendInfos[i] = new SendInfo();
            threads[i] = new Thread(new ThreadTask(rpcClient, messageBytes, sendInfos[i], echoService),
                    "work-thread-" + i);
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

    public static class EchoCallback implements RpcCallback<Echo.EchoResponse> {
        private long startTime;
        private SendInfo sendInfo;

        public EchoCallback(long startTime, SendInfo sendInfo) {
            this.startTime = startTime;
            this.sendInfo = sendInfo;
        }

        @Override
        public void success(Echo.EchoResponse response) {
            if (response != null) {
                sendInfo.successRequestNum++;
                long elapseTimeNs = System.nanoTime() - startTime;
                sendInfo.elapsedNs += elapseTimeNs;
                if (RpcContext.isSet()) {
                    RpcContext rpcContext = RpcContext.getContext();
                    if (rpcContext.getResponseBinaryAttachment() != null) {
                        ReferenceCountUtil.release(rpcContext.getResponseBinaryAttachment());
                    }
                }
                log.debug("async call success, elapseTimeNs={}, response={}",
                        System.nanoTime() - startTime, response.getMessage());
            } else {
                sendInfo.failRequestNum++;
                log.debug("async call failed");
            }
        }

        @Override
        public void fail(Throwable e) {
            sendInfo.failRequestNum++;
            log.debug("async call failed, {}", e.getMessage());
        }
    }

    public static class ThreadTask implements Runnable {

        private RpcClient rpcClient;
        private byte[] messageBytes;
        private SendInfo sendInfo;
        private EchoServiceAsync echoService;

        public ThreadTask(RpcClient rpcClient, byte[] messageBytes,
                          SendInfo sendInfo, EchoServiceAsync echoService) {
            this.rpcClient = rpcClient;
            this.messageBytes = messageBytes;
            this.sendInfo = sendInfo;
            this.echoService = echoService;
        }

        public void run() {
            // build request
            Echo.EchoRequest request = Echo.EchoRequest.newBuilder()
                    .setMessage(new String(messageBytes))
                    .build();
            byte[] attachment = "hello".getBytes();

            while (!stop) {
                try {
                    RpcContext rpcContext = RpcContext.getContext();
                    rpcContext.setRequestBinaryAttachment(attachment);
                    echoService.echo(request, new EchoCallback(System.nanoTime(), sendInfo));
                } catch (Exception ex) {
                    log.info("send exception:", ex);
                    sendInfo.failRequestNum++;
                }
            }
        }
    }
}
