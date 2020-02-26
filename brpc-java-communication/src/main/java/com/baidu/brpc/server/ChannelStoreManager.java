package com.baidu.brpc.server;

import io.netty.channel.Channel;

import java.util.List;
import java.util.Map;

public interface ChannelStoreManager {
    void putChannel(String clientName, Channel channel);

    Channel getChannel(String clientName);

    void removeChannel(Channel channel);

    Map<String, List<Channel>> getChannelMap();
}
