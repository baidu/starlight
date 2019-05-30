package com.baidu.brpc.protocol.nshead;

import com.baidu.brpc.protocol.Options;
import com.baidu.brpc.protocol.Protocol;
import com.baidu.brpc.protocol.ProtocolFactory;

public class NSHeadJsonProtocolFactory implements ProtocolFactory {

    @Override
    public Integer getProtocolType() {
        return Options.ProtocolType.PROTOCOL_NSHEAD_JSON_VALUE;
    }

    @Override
    public Protocol createProtocol(String encoding) {
        return new NSHeadJsonProtocol(encoding);
    }
}
