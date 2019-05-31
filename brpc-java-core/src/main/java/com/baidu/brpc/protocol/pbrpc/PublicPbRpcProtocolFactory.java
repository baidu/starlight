package com.baidu.brpc.protocol.pbrpc;

import com.baidu.brpc.protocol.Options;
import com.baidu.brpc.protocol.Protocol;
import com.baidu.brpc.protocol.ProtocolFactory;
import com.baidu.brpc.protocol.hulu.HuluRpcProtocol;

public class PublicPbRpcProtocolFactory implements ProtocolFactory {

    @Override
    public Integer getProtocolType() {
        return Options.ProtocolType.PROTOCOL_PUBLIC_PBRPC_VALUE;
    }

    @Override
    public Protocol createProtocol(String encoding) {
        return new PublicPbrpcProtocol();
    }
}
