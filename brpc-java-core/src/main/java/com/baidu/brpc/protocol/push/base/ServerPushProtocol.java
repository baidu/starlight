package com.baidu.brpc.protocol.push.base;

import com.baidu.brpc.exceptions.BadSchemaException;
import com.baidu.brpc.protocol.Protocol;
import com.baidu.brpc.protocol.Response;
import com.baidu.brpc.protocol.push.SPHead;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public interface ServerPushProtocol extends Protocol {

    SPHead createSPHead();

    SPHead headFromByteBuf(ByteBuf buf) throws BadSchemaException;

    byte[] headToBytes(SPHead spHead);

    Response decodeServerPushResponse(Object in, ChannelHandlerContext ctx);

}
