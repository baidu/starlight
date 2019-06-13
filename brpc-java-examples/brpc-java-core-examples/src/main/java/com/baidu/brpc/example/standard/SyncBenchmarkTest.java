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

import com.baidu.brpc.RpcContext;
import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.client.channel.ChannelType;
import com.baidu.brpc.client.loadbalance.LoadBalanceStrategy;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.protocol.Options;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by huwenwei on 2017/5/1.
 */
@Slf4j
public class SyncBenchmarkTest {

    private static volatile boolean stop = false;

    public static class SendInfo {
        public long successRequestNum = 0;
        public long failRequestNum = 0;
        public long elapsedNs = 0;
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        if (args.length != 2) {
            System.out.println("usage: BenchmarkTest list://127.0.0.1:8002 threadNum");
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
        RpcClient rpcClient = new RpcClient(args[0], options, null);
        int threadNum = Integer.parseInt(args[1]);

        InputStream inputStream = Thread.currentThread().getClass()
                .getResourceAsStream("/message.txt");
        int length = inputStream.available();
        byte[] messageBytes = new byte[length];
        inputStream.read(messageBytes);
        log.info("message size=" + messageBytes.length);

        EchoService echoService = BrpcProxy.getProxy(rpcClient, EchoService.class);

        SendInfo[] sendInfos = new SendInfo[threadNum];
        Thread[] threads = new Thread[threadNum];

        for (int i = 0; i < threadNum; i++) {
            sendInfos[i] = new SendInfo();
            threads[i] = new Thread(new ThreadTask(i, rpcClient, messageBytes, sendInfos[i], echoService),
                    "work-thread-" + i);
            threads[i].start();
        }
        long lastSuccessRequestNum = 0;
        long lastFailRequestNum = 0;
        long lastElapsedNs = 0;
        while (!stop) {
            long beginTime = System.nanoTime();
            try {
                Thread.sleep(1000);
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
            log.info("success={},fail={},average={}ns",
                    (successNum - lastSuccessRequestNum) * 1000 * 1000 * 1000 / (endTime - beginTime),
                    (failNum - lastFailRequestNum) * 1000 * 1000 * 1000 / (endTime - beginTime),
                    averageElapsedNs);
            lastSuccessRequestNum = successNum;
            lastFailRequestNum = failNum;
            lastElapsedNs = elapseNs;
        }
    }

    public static class ThreadTask implements Runnable {
        private int id;
        private RpcClient rpcClient;
        private byte[] messageBytes;
        private SendInfo sendInfo;
        private EchoService echoService;

        public ThreadTask(int id, RpcClient rpcClient, byte[] messageBytes,
                          SendInfo sendInfo, EchoService echoService) {
            this.id = id;
            this.rpcClient = rpcClient;
            this.messageBytes = messageBytes;
            this.sendInfo = sendInfo;
            this.echoService = echoService;
        }

        public void run() {
            // build request
            Echo.EchoRequest request = Echo.EchoRequest.newBuilder()
                    .setMessage("hello" + id)
                    .build();

            while (!stop) {
                try {
                    RpcContext rpcContext = RpcContext.getContext();
                    rpcContext.setRequestBinaryAttachment(messageBytes);
                    long beginTime = System.nanoTime();
                    Echo.EchoResponse response = echoService.echo(request);
                    if (!response.getMessage().equals(request.getMessage())) {
                        log.warn("id:{} request:{}, response:{}", id, request.getMessage(), response.getMessage());
                    }
                    sendInfo.elapsedNs += (System.nanoTime() - beginTime);
                    sendInfo.successRequestNum++;
                    rpcContext = RpcContext.getContext();
                    if (rpcContext.getResponseBinaryAttachment() != null) {
                        ReferenceCountUtil.release(rpcContext.getResponseBinaryAttachment());
                    }
                } catch (RpcException ex) {
                    log.info("send exception:" + ex.getMessage());
                    sendInfo.failRequestNum++;
                }
            }
        }
    }
}
