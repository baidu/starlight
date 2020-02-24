package com.baidu.brpc.protocol.pbrpc;

import com.baidu.brpc.protocol.BrpcMeta;
import com.baidu.brpc.protocol.standard.Echo;

public interface EchoService {

    /**
     * serviceName是类名，不需要加包名
     * methodName是proto中方法定义的index，下标从0开始
     */
    @BrpcMeta(serviceName = "EchoService", methodName = "0")
    Echo.EchoResponse echo(Echo.EchoRequest request);
}
