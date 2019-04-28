package com.baidu.brpc.client.channel;

import com.baidu.brpc.client.RpcClient;

public class BrpcChannelFactory {
    public static BrpcChannel createChannel(String ip, int port, RpcClient rpcClient) {
        ChannelType channelType = rpcClient.getRpcClientOptions().getChannelType();
        if (channelType == ChannelType.POOLED_CONNECTION) {
            return new BrpcPooledChannel(ip, port, rpcClient);
        } else if (channelType == ChannelType.SINGLE_CONNECTION) {
            return new BrpcSingleChannel(ip, port, rpcClient);
        } else if (channelType == ChannelType.SHORT_CONNECTION) {
            return new BrpcShortChannel(ip, port, rpcClient);
        } else {
            throw new IllegalArgumentException("channel type is not valid:" + channelType.getName());
        }
    }
}
