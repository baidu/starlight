package com.baidu.brpc.client.channel;

import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.protocol.Protocol;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Queue;

@Slf4j
public abstract class AbstractBrpcChannelGroup implements BrpcChannelGroup {

    protected String ip;
    protected int port;
    protected Bootstrap bootstrap;
    protected Protocol protocol;

    public AbstractBrpcChannelGroup(String ip, int port, Bootstrap bootstrap, Protocol protocol) {
        this.ip = ip;
        this.port = port;
        this.bootstrap = bootstrap;
        this.protocol = protocol;
    }


    @Override
    public Channel connect(final String ip, final int port) {
        ChannelFuture future = bootstrap.connect(new InetSocketAddress(ip, port));
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if (channelFuture.isSuccess()) {
                    log.debug("future callback, connect to {}:{} success, channel={}",
                            ip, port, channelFuture.channel());
                } else {
                    log.debug("future callback, connect to {}:{} failed due to {}",
                            ip, port, channelFuture.cause().getMessage());
                }
            }
        });
        future.syncUninterruptibly();
        if (future.isSuccess()) {
            return future.channel();
        } else {
            // throw exception when connect failed to the connection pool acquirer
            log.warn("connect to {}:{} failed, msg={}", ip, port, future.cause().getMessage());
            throw new RpcException(future.cause());
        }
    }

    @Override
    public String getIp() {
        return ip;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public long getFailedNum() {
        return 0;
    }

    @Override
    public void incFailedNum() {

    }

    @Override
    public Queue<Integer> getLatencyWindow() {
        return null;
    }

    @Override
    public void updateLatency(int latency) {

    }

    @Override
    public void updateLatencyWithReadTimeOut() {

    }

    @Override
    public Protocol getProtocol() {
        return protocol;
    }
}
