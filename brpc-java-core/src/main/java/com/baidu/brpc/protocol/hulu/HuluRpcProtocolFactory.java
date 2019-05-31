package com.baidu.brpc.protocol.hulu;

import com.baidu.brpc.protocol.Options;
import com.baidu.brpc.protocol.Protocol;
import com.baidu.brpc.protocol.ProtocolFactory;
import com.baidu.brpc.protocol.sofa.SofaRpcProtocol;

public class HuluRpcProtocolFactory implements ProtocolFactory {

    @Override
    public Integer getProtocolType() {
        return Options.ProtocolType.PROTOCOL_HULU_PBRPC_VALUE;
    }

    @Override
    public Protocol createProtocol(String encoding) {
        return new HuluRpcProtocol();
    }
}
