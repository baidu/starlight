package com.baidu.brpc;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.Setter;

/**
 * runtime information which are not in Request/Response.
 */
@Setter
@Getter
public class Controller {

    private Integer readTimeoutMillis;

    private Integer writeTimeoutMillis;

    private Integer nsHeadLogId;

    private Map<String, String> requestKvAttachment;
    private ByteBuf requestBinaryAttachment;

    private Map<String, String> responseKvAttachment;
    private ByteBuf responseBinaryAttachment;

    private Channel channel;

    private SocketAddress remoteAddress;

    public void setRequestBinaryAttachment(ByteBuf byteBuf) {
        this.requestBinaryAttachment = byteBuf == null ? null : Unpooled.wrappedBuffer(byteBuf);
    }

    public void setRequestBinaryAttachment(byte[] bytes) {
        this.requestBinaryAttachment = bytes == null ? null : Unpooled.wrappedBuffer(bytes);
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
        if (channel != null) {
            ChannelInfo channelInfo = ChannelInfo.getClientChannelInfo(channel);
            if (channelInfo != null) {
                channelInfo.setFromRpcContext(true);
            }
        }
    }

    public String getRemoteHost() {
        if (remoteAddress != null) {
            InetSocketAddress remoteAddress = (InetSocketAddress) this.remoteAddress;
            InetAddress address = remoteAddress.getAddress();
            return address.getHostAddress();
        } else {
            return null;
        }
    }
}
