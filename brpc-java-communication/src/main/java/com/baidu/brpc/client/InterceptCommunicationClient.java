package com.baidu.brpc.client;

import com.baidu.brpc.client.channel.ServiceInstance;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.interceptor.*;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
public class InterceptCommunicationClient extends CommunicationClient {

    public InterceptCommunicationClient(
            ServiceInstance serviceInstance,
            CommunicationOptions communicationOptions,
            List<Interceptor> interceptors) {
        super(serviceInstance, communicationOptions, interceptors);
    }

    public void executeChain(Request request, Response response) throws RpcException {
        InterceptorChain interceptorChain = new DefaultInterceptorChain(this.interceptors);
        try {
            interceptorChain.intercept(request, response);
        } catch (RpcException ex) {
            log.error("exception :", ex);
            throw ex;
        }
    }

}
