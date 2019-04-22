package com.baidu.brpc.interceptor;

import java.util.Iterator;
import java.util.List;

import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;

public class DefaultInterceptorChain implements InterceptorChain {
    private List<Interceptor> interceptors;
    private Iterator<Interceptor> iterator;

    public DefaultInterceptorChain(List<Interceptor> interceptors) {
        this.interceptors = interceptors;
        this.iterator = interceptors.iterator();
    }

    @Override
    public void intercept(Request request, Response response) throws Exception {
        if (iterator.hasNext()) {
            Interceptor interceptor = iterator.next();
            boolean success = interceptor.handleRequest(request);
            if (!success) {
                throw new RpcException(RpcException.INTERCEPT_EXCEPTION, "request intercept fail");
            }
            interceptor.aroundProcess(request, response, this);
            if (request.getCallback() == null) {
                interceptor.handleResponse(response);
            }
        }
    }
}
