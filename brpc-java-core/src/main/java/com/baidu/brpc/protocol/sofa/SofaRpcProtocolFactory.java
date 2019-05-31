package com.baidu.brpc.protocol.sofa;

import com.baidu.brpc.protocol.Options;
import com.baidu.brpc.protocol.Protocol;
import com.baidu.brpc.protocol.ProtocolFactory;

public class SofaRpcProtocolFactory implements ProtocolFactory {

    @Override
    public Integer getProtocolType() {
        return Options.ProtocolType.PROTOCOL_SOFA_PBRPC_VALUE;
    }

    @Override
    public Protocol createProtocol(String encoding) {
        return new SofaRpcProtocol();
    }
}
