package com.baidu.brpc.protocol.dubbo;

import com.alibaba.com.caucho.hessian.io.Hessian2Input;
import com.alibaba.com.caucho.hessian.io.Hessian2Output;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import lombok.Getter;
import lombok.Setter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Setter
@Getter
public class DubboPacket {
    private DubboHeader header;
    private ByteBuf bodyBuf;

    public static byte[] encodeHeartbeatBody() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Hessian2Output hessian2Output = new Hessian2Output(outputStream);
        hessian2Output.writeString(DubboConstants.HEARTBEAT_EVENT);
        hessian2Output.flushBuffer();
        byte[] bodyBytes = outputStream.toByteArray();
        return bodyBytes;
    }

    public static Object decodeEventBody(ByteBuf bodyBuf) throws IOException {
        ByteBufInputStream inputStream = null;
        try {
            inputStream = new ByteBufInputStream(bodyBuf, true);
            Hessian2Input hessian2Input = new Hessian2Input(inputStream);
            Object bodyObject = hessian2Input.readObject();
            return bodyObject;
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }
}
