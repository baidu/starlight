package com.baidu.brpc.protocol.pbrpc;

import com.baidu.brpc.protocol.standard.Echo;
import com.baidu.brpc.protocol.standard.Echo.EchoRequest;
import com.baidu.brpc.protocol.standard.Echo.EchoResponse;

public class EchoServiceImpl implements EchoService {

    @Override
    public EchoResponse echo(EchoRequest request) {
        String message = request.getMessage();
        Echo.EchoResponse response = Echo.EchoResponse.newBuilder()
                .setMessage(message).build();
        return response;
    }
}
