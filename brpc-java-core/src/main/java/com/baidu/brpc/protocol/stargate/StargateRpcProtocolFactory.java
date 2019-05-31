package com.baidu.brpc.protocol.stargate;

import com.baidu.brpc.protocol.Options;
import com.baidu.brpc.protocol.Protocol;
import com.baidu.brpc.protocol.ProtocolFactory;
import com.baidu.brpc.protocol.standard.BaiduRpcProtocol;

public class StargateRpcProtocolFactory implements ProtocolFactory {

    @Override
    public Integer getProtocolType() {
        return Options.ProtocolType.PROTOCOL_STARGATE_VALUE;
    }

    @Override
    public Protocol createProtocol(String encoding) {
        return new StargateRpcProtocol();
    }
}
