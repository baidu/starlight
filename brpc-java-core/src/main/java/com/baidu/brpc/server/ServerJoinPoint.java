package com.baidu.brpc.server;

import com.baidu.brpc.interceptor.AbstractJoinPoint;
import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.protocol.Request;
import org.apache.commons.lang3.Validate;

import java.util.List;

/**
 * JoinPoint implementation for server
 * @author Li Yuanxin(liyuanxin999@163.com)
 */
public class ServerJoinPoint extends AbstractJoinPoint {

    private RpcServer rpcServer;

    public ServerJoinPoint(Request request, RpcServer rpcServer) {
        super(request);
        Validate.notNull(rpcServer, "rpcServer cannot be null");
        this.rpcServer = rpcServer;
    }

    @Override
    protected List<Interceptor> getInterceptors() {
        return rpcServer.getInterceptors();
    }

    @Override
    protected Object internalProceed() throws Exception {
        Request request = getRequest();
        return request.getTargetMethod().invoke(request.getTarget(), request.getArgs());
    }
}
