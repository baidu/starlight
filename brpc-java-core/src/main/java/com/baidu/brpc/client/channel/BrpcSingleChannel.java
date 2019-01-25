package com.baidu.brpc.client.channel;


import com.baidu.brpc.ChannelInfo;
import com.baidu.brpc.client.RpcClient;
import io.netty.channel.Channel;

import java.util.NoSuchElementException;

/**
 * BrpcSingleChannel build single & short connection with server
 * and channel will be closed by brpc after communication with server
 */
public class BrpcSingleChannel extends AbstractBrpcChannelGroup {

    private volatile Channel channel;

    public BrpcSingleChannel(String ip, int port, RpcClient rpcClient) {
        super(ip, port, rpcClient.getBootstrap(), rpcClient.getProtocol());
    }

    @Override
    public Channel getChannel() throws Exception, NoSuchElementException, IllegalStateException {

        if (channel == null || !channel.isActive()) {
            synchronized (this) {
                if (channel != null && !channel.isActive()) {
                    channel.close();
                    channel = null;
                }
                if (channel == null) {
                    channel = connect(ip, port);
                    ChannelInfo channelInfo = ChannelInfo.getOrCreateClientChannelInfo(channel);
                    channelInfo.setProtocol(protocol);
                    channelInfo.setChannelGroup(this);

                }
            }
        }
        return channel;
    }

    @Override
    public void returnChannel(Channel channel) {

    }

    @Override
    public void removeChannel(Channel channel) {
        closeChannel();
    }

    @Override
    public void close() {
        closeChannel();
    }

    @Override
    public void updateMaxConnection(int num) {

        // do nothing

    }

    @Override
    public int getCurrentMaxConnection() {
        return getActiveConnectionNum();
    }

    @Override
    public int getActiveConnectionNum() {
        if (channel != null && channel.isActive()) {
            return 1;
        }

        return 0;
    }

    @Override
    public int getIdleConnectionNum() {
        if (channel == null || !channel.isActive()) {
            return 1;
        }
        return 0;
    }

    private void closeChannel() {
        if (channel != null && channel.isActive()) {
            channel.close();
        }
    }
}
