package com.baidu.brpc.protocol.pbrpc;

import com.baidu.brpc.protocol.nshead.NSHead;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PublicPbRpcPacket {

    private NSHead nsHead;
    private ByteBuf body;
}
