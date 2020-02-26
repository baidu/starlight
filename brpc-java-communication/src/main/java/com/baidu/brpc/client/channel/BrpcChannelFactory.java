package com.baidu.brpc.client.channel;

import com.baidu.brpc.client.CommunicationOptions;

public class BrpcChannelFactory {
    public static BrpcChannel createChannel(ServiceInstance serviceInstance,
                                            CommunicationOptions communicationOptions) {
        ChannelType channelType = communicationOptions.getChannelType();
        if (channelType == ChannelType.POOLED_CONNECTION) {
            return new BrpcPooledChannel(serviceInstance, communicationOptions);
        } else if (channelType == ChannelType.SINGLE_CONNECTION) {
            return new BrpcSingleChannel(serviceInstance, communicationOptions);
        } else if (channelType == ChannelType.SHORT_CONNECTION) {
            return new BrpcShortChannel(serviceInstance, communicationOptions);
        } else {
            throw new IllegalArgumentException("channel type is not valid:" + channelType.getName());
        }
    }
}
