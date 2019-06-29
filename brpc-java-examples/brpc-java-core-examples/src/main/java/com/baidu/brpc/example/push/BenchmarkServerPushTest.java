package com.baidu.brpc.example.push;

import com.baidu.brpc.example.push.normal.EchoServiceImpl;
import com.baidu.brpc.example.push.push.PushData;
import com.baidu.brpc.example.push.push.PushResult;
import com.baidu.brpc.example.push.push.ServerSideUserPushApi;
import com.baidu.brpc.protocol.Options;
import com.baidu.brpc.server.BrpcPushProxy;
import com.baidu.brpc.server.RpcServer;
import com.baidu.brpc.server.RpcServerOptions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class BenchmarkServerPushTest {

    public static class SendInfo {
        public long successRequestNum = 0;
        public long failRequestNum = 0;
        public long elapsedNs = 0;
    }

    private static volatile boolean stop = false;

    // rpc press: send pressure to RpcClientTest
    // push & rpc press: send pressure to BenchmarkClientPushTest

    public static void main(String[] args) throws InterruptedException {

        int port = 8012;

        if (args.length != 1) {
            System.out.println("usage: threadNum");
            System.exit(-1);
        }

        int threadNum = Integer.parseInt(args[0]);

        RpcServerOptions options = new RpcServerOptions();
        options.setReceiveBufferSize(64 * 1024 * 1024);
        options.setSendBufferSize(64 * 1024 * 1024);
        options.setKeepAliveTime(20);
        options.setProtocolType(Options.ProtocolType.PROTOCOL_SERVER_PUSH_VALUE);

        final RpcServer rpcServer = new RpcServer(port, options);
        rpcServer.registerService(new EchoServiceImpl());
        ServerSideUserPushApi pushApi = BrpcPushProxy.getProxy(rpcServer, ServerSideUserPushApi.class);
        rpcServer.start();

        // wait until the client connected to server
        while (!EchoServiceImpl.clientStarted) {
            Thread.sleep(1000);
        }

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

        log.info("message_1k: {}", new String(messageBytes));

        SendInfo[] sendInfos = new SendInfo[threadNum];
        Thread[] threads = new Thread[threadNum];

        for (int i = 0; i < threadNum; i++) {
            sendInfos[i] = new SendInfo();

            threads[i] = new Thread(new ServerPushTask(messageBytes, sendInfos[i], pushApi));
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
                ++ second;
            } catch (Exception e) {
                log.error(e.getMessage());
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


            // 1s : success=request/s; fail=request/s; average= 每个请求的耗时/ns
            String msg = String.format("success=%s,fail=%s,average=%sns",
                    (successNum - lastSuccessRequestNum) * 1000 * 1000 * 1000 / (endTime - beginTime),
                    (failNum - lastFailRequestNum) * 1000 * 1000 * 1000 / (endTime - beginTime),
                    averageElapsedNs);
            lastSuccessRequestNum = successNum;
            lastFailRequestNum = failNum;
            lastElapsedNs = elapseNs;

            if ( second > 30 ) {
                long avgQps = (lastSuccessRequestNum - skippedQps) / (second - 30);
                msg = msg + ",avgQps=" + avgQps;
//                log.info("skippedQps:{}, avgQps:{}", skippedQps, avgQps);
            } else {
                skippedQps = lastSuccessRequestNum;
            }

            log.info(msg);

        }
    }

    public static class ServerPushTask implements Runnable {
        private byte[] messageBytes;
        private SendInfo sendInfo;
        private ServerSideUserPushApi serverSideUserPushApi;

        public ServerPushTask(byte[] messageBytes,
                              SendInfo sendInfo, ServerSideUserPushApi serverSideUserPushApi) {
            this.messageBytes = messageBytes;
            this.sendInfo = sendInfo;
            this.serverSideUserPushApi = serverSideUserPushApi;
        }

        public void run() {
            PushData p = new PushData();
            p.setData(new String(messageBytes));

            while (!stop) {
                try {
                    long startTime = System.nanoTime();
                    PushResult r = serverSideUserPushApi.clientReceive("Benchmark",
                            "extra", p);
                    if (r != null && StringUtils.isNotEmpty(r.getResult()) ) {
                        sendInfo.successRequestNum++;
                        long elapseTimeNs = System.nanoTime() - startTime;
                        sendInfo.elapsedNs += elapseTimeNs;
                        log.debug("sync call success, elapseTimeNs={} ",
                                System.nanoTime() - startTime);
                    } else {
                        sendInfo.failRequestNum++;
                        log.debug("sync call failed");
                    }
                } catch (Exception ex) {
                    log.info("send exception:", ex);
                    sendInfo.failRequestNum++;
                }
            }
        }
    }


}
