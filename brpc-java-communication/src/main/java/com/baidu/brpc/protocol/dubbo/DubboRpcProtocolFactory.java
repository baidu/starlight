package com.baidu.brpc.protocol.dubbo;

import com.baidu.brpc.protocol.Options;
import com.baidu.brpc.protocol.Protocol;
import com.baidu.brpc.protocol.ProtocolFactory;

public class DubboRpcProtocolFactory implements ProtocolFactory {

    @Override
    public Integer getProtocolType() {
        return Options.ProtocolType.PROTOCOL_DUBBO_VALUE;
    }

    public Integer getPriority() {
        return ProtocolFactory.DEFAULT_PRIORITY;
    }

    @Override
    public Protocol createProtocol(String encoding) {
        return new DubboRpcProtocol();
    }

    @Override
    public String getProtocolName() {
        return Options.ProtocolType.PROTOCOL_DUBBO.name();
    }
}
