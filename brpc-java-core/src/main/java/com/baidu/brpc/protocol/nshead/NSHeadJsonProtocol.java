package com.baidu.brpc.protocol.nshead;

import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.exceptions.RpcException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

@Slf4j
public class NSHeadJsonProtocol extends NSHeadRpcProtocol {

    private static final Gson gson = (new GsonBuilder())
            .disableHtmlEscaping()
            .serializeSpecialFloatingPointValues()
            .create();

    public NSHeadJsonProtocol(String encoding) {
        super(encoding);
    }

    @Override
    public byte[] encodeBody(Object body, RpcMethodInfo rpcMethodInfo) {
        Validate.notNull(body, "body must not be empty");
        byte[] bytes;
        String json = gson.toJson(body);
        try {
            bytes = json.getBytes(this.encoding);
        } catch (Exception e) {
            log.error("can not serialize object using mcpack", e);
            throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e);
        }

        return bytes;
    }

    @Override
    public Object decodeBody(ByteBuf bodyBuf, RpcMethodInfo rpcMethodInfo) {
        try {
            Object result;
            try {
                int readableBytes = bodyBuf.readableBytes();
                byte[] bodyBytes = new byte[readableBytes];
                bodyBuf.readBytes(bodyBytes);
                String jsonString = new String(bodyBytes, this.encoding);
                if (rpcMethodInfo.getTarget() != null) {
                    // server端
                    result = gson.fromJson(jsonString, rpcMethodInfo.getInputClasses()[0]);
                } else {
                    result = gson.fromJson(jsonString, rpcMethodInfo.getOutputClass());
                }
            } catch (Exception e) {
                log.error("can not deserialize object", e);
                throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e);
            }
            return result;
        } finally {
            if (bodyBuf != null) {
                bodyBuf.release();
            }
        }
    }

}
