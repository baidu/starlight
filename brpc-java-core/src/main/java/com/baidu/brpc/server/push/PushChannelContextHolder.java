package com.baidu.btcc.rpc.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;

public class ChannelContextHolder {
    public static final AttributeKey<String> PARTICIPANT_KEY = AttributeKey.valueOf("participant");
    private static final ThreadLocal<ChannelHandlerContext> CURRENT_CHANNEL = new ThreadLocal<>();

    public static void setCurrentChannel(ChannelHandlerContext ctx) {
        CURRENT_CHANNEL.set(ctx);
    }

    public static ChannelHandlerContext getCurrentChannel() {
        return CURRENT_CHANNEL.get();
    }

    public static void clear() {
        CURRENT_CHANNEL.remove();
    }
}
