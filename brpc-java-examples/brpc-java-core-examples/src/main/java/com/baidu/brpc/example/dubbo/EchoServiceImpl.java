package com.baidu.brpc.example.dubbo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EchoServiceImpl implements EchoService {
    @Override
    public EchoResponse echo(EchoRequest request) {
//        log.debug("receive request:{}", request.getMessage());
        EchoResponse response = new EchoResponse();
        response.setMessage(request.getMessage());
        return response;
    }
}
