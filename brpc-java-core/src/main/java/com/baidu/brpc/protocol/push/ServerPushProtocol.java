package com.baidu.brpc.protocol.push;

import com.baidu.brpc.exceptions.BadSchemaException;
import com.baidu.brpc.protocol.Protocol;
import com.baidu.brpc.protocol.Response;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public interface ServerPushProtocol extends Protocol {

    SPHead createSPHead();

    SPHead headFromByteBuf(ByteBuf buf) throws BadSchemaException;

    ByteBuf headToBytes(SPHead spHead);

    Response decodeServerPushResponse(Object in, ChannelHandlerContext ctx);

}
