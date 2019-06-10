package com.baidu.brpc.protocol.dubbo;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DubboPacket {
    private DubboHeader header;
    private byte[] bodyBytes;
}
