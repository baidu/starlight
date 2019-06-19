package com.baidu.brpc.server;

import com.baidu.brpc.server.push.PushChannelContextHolder;
import io.netty.channel.Channel;
import io.netty.util.Attribute;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class DefaultChannelStoreManager implements ChannelStoreManager {
    private Map<String, List<Channel>> channelMap = new HashMap<String, List<Channel>>();
    private Set<Channel> channelSet = new HashSet<Channel>();
    private AtomicInteger index = new AtomicInteger(0);


    @Override
    public void putChannel(String clientName, Channel channel) {
        if (!channelSet.contains(channel)) {
            List<Channel> channelList = channelMap.get(clientName);
            if (channelList == null) {
                channelList = new ArrayList<Channel>();
                channelMap.put(clientName, channelList);
            }
            channelMap.get(clientName).add(channel);
        }
    }

    @Override
    public Channel getChannel(String clientName) {
        List<Channel> channelList = channelMap.get(clientName);
        if (channelList == null || channelList.size() == 0) {
            log.info("no available connection for clientName={}", clientName);
            return null;
        }
        int id = index.getAndIncrement() % channelList.size();
        Channel channel = channelList.get(id);
        return channel;
    }

    @Override
    public void removeChannel(Channel channel) {
        Attribute<String> participant = channel.attr(PushChannelContextHolder.CLIENTNAME_KEY);
        String participantName = participant.get();
        List<Channel> channelList = channelMap.get(participantName);
        channelList.remove(channel);
        channelSet.remove(channel);

    }

    @Override
    public Map<String, List<Channel>> getChannelMap() {
        return channelMap;
    }


}
