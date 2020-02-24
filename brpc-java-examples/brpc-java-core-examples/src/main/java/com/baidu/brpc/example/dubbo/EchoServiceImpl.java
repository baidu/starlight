package com.baidu.brpc.example.dubbo;

public class EchoServiceImpl implements EchoService {
    @Override
    public EchoResponse echo(EchoRequest request) {
        System.out.println("receive request:" + request.getMessage());
        EchoResponse response = new EchoResponse();
        response.setMessage(request.getMessage());
        return response;
    }
}
