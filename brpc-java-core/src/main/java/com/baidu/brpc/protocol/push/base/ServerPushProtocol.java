package com.baidu.brpc.protocol.push.base;

import com.baidu.brpc.exceptions.BadSchemaException;
import com.baidu.brpc.protocol.Protocol;
import com.baidu.brpc.protocol.push.SPBody;
import com.baidu.brpc.protocol.push.SPHead;

import io.netty.buffer.ByteBuf;

public interface ServerPushProtocol extends Protocol {

    SPHead createSPHead();

    SPBody createSPBody();

    SPHead headFromByteBuf(ByteBuf buf) throws BadSchemaException;

    byte[] headToBytes(SPHead spHead);

}
