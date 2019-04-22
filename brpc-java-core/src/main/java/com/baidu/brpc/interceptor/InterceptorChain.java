package com.baidu.brpc.interceptor;

import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;

public interface InterceptorChain {
    void intercept(Request request, Response response) throws Exception;
}
