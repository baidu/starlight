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

package com.baidu.brpc.client;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.baidu.brpc.client.instance.Endpoint;
import com.baidu.brpc.protocol.standard.Echo;
import com.baidu.brpc.protocol.standard.EchoService;
import com.baidu.brpc.protocol.standard.EchoServiceImpl;
import com.baidu.brpc.server.RpcServer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientInitTest {


    @Test
    public void testClientInit() {

        RpcServer rpcServer = new RpcServer(8000);
        rpcServer.registerService(new EchoServiceImpl());
        rpcServer.start();

        // new first rpc client
        long firstStartTime = System.currentTimeMillis();
        RpcClient firstRpcClient = new RpcClient("list://127.0.0.1:8000");
        long firstEndTime = System.currentTimeMillis();

        EchoService echoService = BrpcProxy.getProxy(firstRpcClient, EchoService.class);
        Echo.EchoRequest request = Echo.EchoRequest.newBuilder().setMessage("hello").build();
        Echo.EchoResponse response = echoService.echo(request);
        assertEquals("hello", response.getMessage());
        log.info("first new rpcClient cost : {}", firstEndTime - firstStartTime);
        firstRpcClient.stop();

        // new second rpc client
        long secondStartTime = System.currentTimeMillis();
        RpcClient secondRpcClient = new RpcClient("list://127.0.0.1:8000");
        long secondEndTime = System.currentTimeMillis();
        log.info("second new rpcClient cost : {}", secondEndTime - secondStartTime);
        echoService = BrpcProxy.getProxy(secondRpcClient, EchoService.class);
        request = Echo.EchoRequest.newBuilder().setMessage("hello").build();
        response = echoService.echo(request);
        assertEquals("hello", response.getMessage());
        secondRpcClient.stop();

        // new third rpc client for short connection
        Endpoint endPoint = new Endpoint("127.0.0.1", 8000);
        long thirdStartTime = System.currentTimeMillis();
        RpcClient thirdConnectionRpcClient = new RpcClient(endPoint);
        long thirdEndTime = System.currentTimeMillis();
        log.info("third new rpcClient cost : {}", thirdEndTime - thirdStartTime);
        thirdConnectionRpcClient.stop();

        ThreadNumStat stat = calThreadNum();
        int processors = Runtime.getRuntime().availableProcessors();

        Assert.assertEquals(processors, stat.ioThreadNum);
        Assert.assertEquals(processors, stat.workThreadNum);
        Assert.assertEquals(1, stat.timoutThreadNum);

        rpcServer.shutdown();
    }


    private ThreadNumStat calThreadNum() {
        ThreadNumStat stat = new ThreadNumStat();

        // stat all thread
        Map<Thread, StackTraceElement[]> allThreadMap = Thread.getAllStackTraces();
        for (Map.Entry<Thread, StackTraceElement[]> entry : allThreadMap.entrySet()) {

            Thread thread = entry.getKey();

            if (thread.getName().contains("invalid-channel-callback-thread")) {
                stat.callBackThreadNum ++;
            } else if (thread.getName().contains("brpc-io-thread")) {
                stat.ioThreadNum ++;
            } else if (thread.getName().contains("brpc-work-thread")) {
                stat.workThreadNum ++;
            } else if (thread.getName().contains("health-check-timer-thread")) {
                stat.healthCheckThreadNum ++;
            } else if (thread.getName().contains("timeout-timer-thread")) {
                stat.timoutThreadNum ++;
            }
        }

        log.info("thread statistic data, callBackThreadNum : {}, \n ioThreadNum : {}, \n workThreadNum : {}, \n"
                        + " healthCheckThreadNum : {}, \n timeoutThreadNum : {} \n", stat.callBackThreadNum,
                stat.ioThreadNum, stat.workThreadNum, stat.healthCheckThreadNum, stat.timoutThreadNum);

        return stat;
    }


    public static class ThreadNumStat {
        public int callBackThreadNum;
        public int ioThreadNum;
        public int workThreadNum;
        public int healthCheckThreadNum;
        public int timoutThreadNum;
    }


}
