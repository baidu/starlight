package com.baidu.brpc.protocol.http;

import com.baidu.brpc.protocol.Options;
import com.baidu.brpc.protocol.Protocol;
import com.baidu.brpc.protocol.ProtocolFactory;

public class HttpProtobufProtocolFactory implements ProtocolFactory {

    @Override
    public Integer getProtocolType() {
        return Options.ProtocolType.PROTOCOL_HTTP_PROTOBUF_VALUE;
    }

    public Integer getPriority() {
        return ProtocolFactory.DEFAULT_PRIORITY - 1;
    }

    @Override
    public Protocol createProtocol(String encoding) {
        return new HttpRpcProtocol(getProtocolType(), encoding);
    }

    @Override
    public String getProtocolName() {
        return Options.ProtocolType.PROTOCOL_HTTP_PROTOBUF.name();
    }
}
