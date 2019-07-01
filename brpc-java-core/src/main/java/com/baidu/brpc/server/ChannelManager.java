package com.baidu.brpc.server;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.StringUtils;

import com.baidu.brpc.server.push.PushChannelContextHolder;

import io.netty.channel.Channel;
import io.netty.util.Attribute;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChannelManager {
    private static volatile ChannelManager instance;
    private static ChannelStoreManager storeManager;


    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private ChannelStoreManager innerStoreManager;

    public static void setStoreManager(ChannelStoreManager manager) {
        storeManager = manager;
    }

    public static ChannelManager getInstance() {
        if (instance == null) {
            synchronized (ChannelManager.class) {
                if (instance == null) {
                    if (storeManager != null) {
                        instance = new ChannelManager(storeManager);
                    } else {
                        instance = new ChannelManager(new DefaultChannelStoreManager());
                    }
                }
            }
        }
        return instance;
    }

    private ChannelManager(ChannelStoreManager storeManager) {
        this.innerStoreManager = storeManager;
    }

    public void putChannel(String clientName, Channel channel) {
        lock.writeLock().lock();
        innerStoreManager.putChannel(clientName, channel);
        lock.writeLock().unlock();
    }

    public Channel getChannel(String clientName) {
        if (log.isDebugEnabled()) {
            for (Map.Entry<String, List<Channel>> entry : innerStoreManager.getChannelMap().entrySet()) {
                log.debug("participantName={}, channelNum={}",
                        entry.getKey(),
                        entry.getValue() == null ? 0 : entry.getValue().size());
            }
        }
        lock.readLock().lock();
        try {
            return innerStoreManager.getChannel(clientName);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void removeChannel(Channel channel) {

        Attribute<String> participant = channel.attr(PushChannelContextHolder.CLIENTNAME_KEY);
        String participantName = participant.get();
        if (StringUtils.isNotBlank(participantName)) {
            lock.writeLock().lock();
            innerStoreManager.removeChannel(channel);
            lock.writeLock().unlock();
        }
    }

    public Map<String, List<Channel>> getChannelMap() {
        return innerStoreManager.getChannelMap();
    }


}
