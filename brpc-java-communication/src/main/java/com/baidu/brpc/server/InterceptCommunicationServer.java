package com.baidu.brpc.server;

import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.interceptor.*;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import com.baidu.brpc.utils.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class InterceptCommunicationServer extends CommunicationServer {
    private List<Interceptor> interceptors = new ArrayList<Interceptor>();

    public InterceptCommunicationServer(int port) {
        this(null, port, new RpcServerOptions(), null);
    }

    public InterceptCommunicationServer(String host, int port) {
        this(host, port, new RpcServerOptions(), null);
    }

    public InterceptCommunicationServer(int port, RpcServerOptions options) {
        this(null, port, options, null);
    }

    public InterceptCommunicationServer(String host, int port, RpcServerOptions options) {
        this(host, port, options, null);
    }

    public InterceptCommunicationServer(int port, RpcServerOptions options, List<Interceptor> interceptors) {
        this(null, port, options, interceptors);
    }

    public InterceptCommunicationServer(String host, int port,
                               final RpcServerOptions options,
                               List<Interceptor> interceptors) {
        super(host, port, options);
        if (CollectionUtils.isNotEmpty(interceptors)) {
            this.interceptors.addAll(interceptors);
        }
        this.interceptors.add(new ServerTraceInterceptor());
        this.interceptors.add(new ServerInvokeInterceptor());
    }

    public void execute(Request request, Response response) throws RpcException {
        InterceptorChain interceptorChain = new DefaultInterceptorChain(this.interceptors);
        interceptorChain.intercept(request, response);
    }

}
