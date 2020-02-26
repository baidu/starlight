package com.baidu.brpc.protocol.hulu;

import com.baidu.brpc.protocol.Options;
import com.baidu.brpc.protocol.Protocol;
import com.baidu.brpc.protocol.ProtocolFactory;

public class HuluRpcProtocolFactory implements ProtocolFactory {

    @Override
    public Integer getProtocolType() {
        return Options.ProtocolType.PROTOCOL_HULU_PBRPC_VALUE;
    }

    public Integer getPriority() {
        return ProtocolFactory.DEFAULT_PRIORITY;
    }

    @Override
    public Protocol createProtocol(String encoding) {
        return new HuluRpcProtocol();
    }

    @Override
    public String getProtocolName() {
        return Options.ProtocolType.PROTOCOL_HULU_PBRPC.name();
    }
}
