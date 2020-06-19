package com.baidu.brpc;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.util.concurrent.FastThreadLocal;
import lombok.Getter;
import lombok.Setter;

/**
 * runtime information which are not in Request/Response.
 * it should be reset when begin new rpc.
 * the requestBinaryAttachment should be released at server.
 * the responseBinaryAttachment should be released at client.
 */
@Setter
@Getter
public class RpcContext {
    private static final FastThreadLocal<RpcContext> CURRENT_RPC_CONTEXT = new FastThreadLocal<RpcContext>() {
        @Override
        protected RpcContext initialValue() {
            return new RpcContext();
        }
    };

    public static boolean isSet() {
        return CURRENT_RPC_CONTEXT.isSet();
    }

    public static RpcContext getContext() {
        return CURRENT_RPC_CONTEXT.get();
    }

    public static void removeContext() {
        CURRENT_RPC_CONTEXT.remove();
    }

    private Integer readTimeoutMillis;

    private Integer writeTimeoutMillis;

    /**
     * logId of protocol, application can set it.
     */
    private Long logId;

    /**
     * set custom service instance tag,
     * so that load balance can select instance with the tag.
     */
    private String serviceTag;

    private Map<String, Object> requestKvAttachment;
    private ByteBuf requestBinaryAttachment;

    private Map<String, Object> responseKvAttachment;
    private ByteBuf responseBinaryAttachment;

    private Channel channel;

    private SocketAddress remoteAddress;

    public void reset() {
        readTimeoutMillis = null;
        writeTimeoutMillis = null;
        logId = null;
        requestKvAttachment = null;
        requestBinaryAttachment = null;
        responseBinaryAttachment = null;
        responseKvAttachment = null;
        channel = null;
        remoteAddress = null;
        serviceTag = null;
    }

    public void setRequestBinaryAttachment(ByteBuf byteBuf) {
        this.requestBinaryAttachment = byteBuf == null ? null : Unpooled.wrappedBuffer(byteBuf);
    }

    public void setRequestBinaryAttachment(byte[] bytes) {
        this.requestBinaryAttachment = bytes == null ? null : Unpooled.wrappedBuffer(bytes);
    }

    public void setRequestKvAttachment(String key, Object value) {
        if (requestKvAttachment == null) {
            requestKvAttachment = new HashMap<String, Object>();
        }
        requestKvAttachment.put(key, value);
    }

    public void setRequestKvAttachment(Map<String, Object> attachment) {
        if (requestKvAttachment == null) {
            requestKvAttachment = attachment;
        } else {
            requestKvAttachment.putAll(attachment);
        }
    }

    public void setResponseKvAttachment(String key, Object value) {
        if (responseKvAttachment == null) {
            responseKvAttachment = new HashMap<String, Object>();
        }
        responseKvAttachment.put(key, value);
    }

    public void setResponseKvAttachment(Map<String, Object> attachment) {
        if (responseKvAttachment == null) {
            responseKvAttachment = attachment;
        } else {
            responseKvAttachment.putAll(attachment);
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
