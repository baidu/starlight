package com.baidu.brpc.protocol;

public interface ProtocolFactory {
    Integer getProtocolType();
    Protocol createProtocol(String encoding);
}
