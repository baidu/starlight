package com.baidu.brpc.protocol.nshead;

import com.baidu.brpc.protocol.Options;
import com.baidu.brpc.protocol.Protocol;
import com.baidu.brpc.protocol.ProtocolFactory;

public class NSHeadProtobufProtocolFactory implements ProtocolFactory {

    @Override
    public Integer getProtocolType() {
        return Options.ProtocolType.PROTOCOL_NSHEAD_PROTOBUF_VALUE;
    }

    public Integer getPriority() {
        return ProtocolFactory.DEFAULT_PRIORITY;
    }

    @Override
    public Protocol createProtocol(String encoding) {
        return new NSHeadProtobufProtocol(encoding);
    }

    @Override
    public String getProtocolName() {
        return Options.ProtocolType.PROTOCOL_NSHEAD_PROTOBUF.name();
    }
}
