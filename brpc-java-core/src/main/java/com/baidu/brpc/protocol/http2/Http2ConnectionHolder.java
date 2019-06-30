package com.baidu.brpc.protocol.http2;

import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Http2ConnectionHolder {
    private boolean isConnected;
    private Http2Connection connection;
    private Http2ConnectionEncoder encoder;
    private Http2ConnectionDecoder decoder;
    //private
}
