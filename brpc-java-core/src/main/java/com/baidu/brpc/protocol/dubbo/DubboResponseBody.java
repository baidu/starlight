package com.baidu.brpc.protocol.dubbo;

import com.alibaba.com.caucho.hessian.io.Hessian2Input;
import com.alibaba.com.caucho.hessian.io.Hessian2Output;
import lombok.Getter;
import lombok.Setter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@Setter
@Getter
public class DubboResponseBody {
    private byte responseType;
    private Object result;
    private Map<String, String> attachments;

    public byte[] encodeResponseBody() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Hessian2Output hessian2Output = new Hessian2Output(outputStream);
        hessian2Output.writeInt(responseType);
        hessian2Output.writeObject(result);
        if (attachments != null) {
            hessian2Output.writeObject(attachments);
        }
        hessian2Output.flushBuffer();
        return outputStream.toByteArray();
    }

    public static byte[] encodeErrorResponseBody(String errorMessage) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Hessian2Output hessian2Output = new Hessian2Output(outputStream);
        hessian2Output.writeString(errorMessage);
        hessian2Output.flushBuffer();
        byte[] bodyBytes = outputStream.toByteArray();
        return bodyBytes;
    }

    public static byte[] encodeHeartbeatResponseBody() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Hessian2Output hessian2Output = new Hessian2Output(outputStream);
        hessian2Output.writeString(DubboConstants.HEARTBEAT_EVENT);
        hessian2Output.flushBuffer();
        byte[] bodyBytes = outputStream.toByteArray();
        return bodyBytes;
    }

    public static DubboResponseBody decodeResponseBody(
            DubboHeader header, byte[] responseBodyBytes) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(responseBodyBytes);
        Hessian2Input hessian2Input = new Hessian2Input(inputStream);
        DubboResponseBody responseBody = new DubboResponseBody();
        responseBody.setResponseType((byte) hessian2Input.readInt());
        responseBody.setResult(hessian2Input.readObject());
        if (header.getFlag() == DubboConstants.RESPONSE_VALUE_WITH_ATTACHMENTS) {
            Map<String, String> map = (Map<String, String>) hessian2Input.readObject(Map.class);
            if (map != null && map.size() > 0) {
                responseBody.getAttachments().putAll(map);
            }
        }
        return responseBody;
    }
}
