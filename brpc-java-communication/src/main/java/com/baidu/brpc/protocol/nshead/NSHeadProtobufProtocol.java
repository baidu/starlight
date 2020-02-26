package com.baidu.brpc.protocol.nshead;

import com.baidu.brpc.RpcMethodInfo;
import com.baidu.brpc.exceptions.RpcException;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.io.IOException;

@Slf4j
public class NSHeadProtobufProtocol extends NSHeadRpcProtocol {

    public NSHeadProtobufProtocol(String encoding) {
        super(encoding);
    }

    @Override
    public byte[] encodeBody(Object body, RpcMethodInfo rpcMethodInfo) {
        Validate.notNull(body, "body must not be empty");
        byte[] bytes;
        try {
            if (rpcMethodInfo.getTarget() != null) {
                // server端，所以是encode response
                bytes = rpcMethodInfo.outputEncode(body);
            } else {
                bytes = rpcMethodInfo.inputEncode(body);
            }
        } catch (IOException ex) {
            throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, ex);
        }

        return bytes;
    }

    @Override
    public Object decodeBody(ByteBuf bodyBuf, RpcMethodInfo rpcMethodInfo) {
        try {
            Object result;
            try {
                if (rpcMethodInfo.getTarget() != null) {
                    // server端，所以是decode request
                    result = rpcMethodInfo.inputDecode(bodyBuf);
                } else {
                    result = rpcMethodInfo.outputDecode(bodyBuf);
                }
            } catch (IOException e) {
                log.warn("invoke parseFrom method error", e);
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
