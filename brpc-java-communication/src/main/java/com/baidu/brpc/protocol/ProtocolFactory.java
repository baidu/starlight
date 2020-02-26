package com.baidu.brpc.protocol;

public interface ProtocolFactory {
    /**
     * protocol priority controls the order of server parsing request.
     * the lower priority will be parsed earlier.
     */
    Integer DEFAULT_PRIORITY = 100;

    Integer getProtocolType();
    Integer getPriority();
    Protocol createProtocol(String encoding);
    String getProtocolName();
}
