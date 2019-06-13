package com.baidu.brpc.server.push;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;

public class PushChannelContextHolder {

    public static final AttributeKey<String> CLIENTNAME_KEY = AttributeKey.valueOf("clientName");
    private static final ThreadLocal<ChannelHandlerContext> CURRENT_CHANNEL = new ThreadLocal<ChannelHandlerContext>();

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
