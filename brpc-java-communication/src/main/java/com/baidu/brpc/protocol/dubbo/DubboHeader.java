package com.baidu.brpc.protocol.dubbo;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DubboHeader {
    private short magic = DubboConstants.MAGIC;
    private byte flag;
    private byte status;
    private long correlationId;
    private int bodyLength;

    public ByteBuf encode() {
        ByteBuf byteBuf = Unpooled.buffer(DubboConstants.FIXED_HEAD_LEN);
        byteBuf.writeShort(DubboConstants.MAGIC);
        byteBuf.writeByte(flag);
        byteBuf.writeByte(status);
        byteBuf.writeLong(correlationId);
        byteBuf.writeInt(bodyLength);
        return byteBuf;
    }

    public static DubboHeader decode(ByteBuf byteBuf) {
        DubboHeader header = new DubboHeader();
        header.setMagic(byteBuf.readShort());
        header.setFlag(byteBuf.readByte());
        header.setStatus(byteBuf.readByte());
        header.setCorrelationId(byteBuf.readLong());
        header.setBodyLength(byteBuf.readInt());
        return header;
    }

}
