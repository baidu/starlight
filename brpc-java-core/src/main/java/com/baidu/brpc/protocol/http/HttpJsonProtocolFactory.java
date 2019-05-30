package com.baidu.brpc.protocol.http;

import com.baidu.brpc.protocol.Options;
import com.baidu.brpc.protocol.Protocol;
import com.baidu.brpc.protocol.ProtocolFactory;

public class HttpJsonProtocolFactory implements ProtocolFactory {

    @Override
    public Integer getProtocolType() {
        return Options.ProtocolType.PROTOCOL_HTTP_JSON_VALUE;
    }

    @Override
    public Protocol createProtocol(String encoding) {
        return new HttpRpcProtocol(getProtocolType(), encoding);
    }
}
