package com.baidu.brpc.interceptor;

import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;

public class ServerInvokeInterceptor extends AbstractInterceptor {
    @Override
    public void aroundProcess(Request request, Response response, InterceptorChain chain) throws Exception {
        if (request.getRpcMethodInfo().isIncludeController()) {
            Object[] args = new Object[request.getArgs().length + 1];
            args[0] = request.getController();
            for (int i = 0; i < request.getArgs().length; i++) {
                args[i + 1] = request.getArgs()[i];
            }
            response.setResult(request.getTargetMethod().invoke(request.getTarget(), args));
        } else {
            response.setResult(request.getTargetMethod().invoke(request.getTarget(), request.getArgs()));
        }
    }
}
