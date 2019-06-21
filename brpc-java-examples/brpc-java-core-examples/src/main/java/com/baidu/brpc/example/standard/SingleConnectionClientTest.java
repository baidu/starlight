package com.baidu.brpc.example.standard;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.baidu.brpc.RpcContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.client.channel.ChannelType;
import com.baidu.brpc.client.loadbalance.LoadBalanceType;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.protocol.Options;
import com.google.common.collect.Lists;

import io.netty.util.ReferenceCountUtil;

@SuppressWarnings("unchecked")
public class SingleConnectionClientTest {

    private static final Logger LOG = LoggerFactory.getLogger(SingleConnectionClientTest.class);

    public static final int PERIOD = 1000;

    public static void main(String[] args) {

        String serviceUrl = "list://127.0.0.1:8002";

        List<Interceptor> interceptors = Lists.newArrayList();
        RpcClient rpcClient = new RpcClient(serviceUrl, getRpcClientOptions(), interceptors);

        // build request
        final Echo.EchoRequest request = Echo.EchoRequest.newBuilder()
                .setMessage("test single connection").build();

        // sync call
        final EchoService echoService = BrpcProxy.getProxy(rpcClient, EchoService.class);
        ScheduledExecutorService schedule = Executors.newScheduledThreadPool(10);
        final AtomicInteger counter = new AtomicInteger(0);
        final Random random = new Random(System.currentTimeMillis());

        schedule.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    int index = counter.getAndIncrement();
                    int countInHalfMinute = 30 * 1000000 / PERIOD;
                    if (((index / countInHalfMinute) & 1) == 1) {
                        return;
                    }
                    RpcContext rpcContext = RpcContext.getContext();
                    rpcContext.setRequestBinaryAttachment("example attachment".getBytes());
                    Echo.EchoResponse response = echoService.echo(request);
                    // sample log
                    if (random.nextInt(10000) < 30) {
                        LOG.info("sync call service=EchoService.echo success, "
                                        + "request={},response={}",
                                request.getMessage(), response.getMessage());
                    }
                    rpcContext = RpcContext.getContext();
                    if (rpcContext.getResponseBinaryAttachment() != null) {
                        LOG.info("attachment="
                                + new String(rpcContext.getResponseBinaryAttachment().array()));
                        ReferenceCountUtil.release(rpcContext.getResponseBinaryAttachment());
                    }
                } catch (RpcException ex) {
                    if (random.nextInt(10000) < 30) {
                        LOG.error("sync call failed, ex=" + ex.getMessage());
                    }
                } catch (Exception e) {
                    LOG.info("other exception, {}", e);
                }
            }
        }, 100000, PERIOD, TimeUnit.MICROSECONDS);

        while (counter.get() < 1000000) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            LOG.info("count:{}", counter.get());
        }
        schedule.shutdown();
        rpcClient.stop();
    }

    private static RpcClientOptions getRpcClientOptions() {
        RpcClientOptions clientOption = new RpcClientOptions();
        clientOption.setProtocolType(Options.ProtocolType.PROTOCOL_BAIDU_STD_VALUE);
        clientOption.setWriteTimeoutMillis(1000);
        clientOption.setReadTimeoutMillis(1000);
        clientOption.setMaxTotalConnections(1000);
        clientOption.setMinIdleConnections(10);
        clientOption.setLoadBalanceType(LoadBalanceType.ROUND_ROBIN.getId());
        clientOption.setCompressType(Options.CompressType.COMPRESS_TYPE_NONE);
        clientOption.setChannelType(ChannelType.SINGLE_CONNECTION);
        clientOption.setKeepAliveTime(25);
        return clientOption;
    }
}
