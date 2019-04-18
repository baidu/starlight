package com.baidu.brpc.server;

import java.util.List;

import org.apache.commons.lang3.Validate;

import com.baidu.brpc.Controller;
import com.baidu.brpc.interceptor.AbstractJoinPoint;
import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.protocol.Request;

/**
 * JoinPoint implementation for server
 * @author Li Yuanxin(liyuanxin@baidu.com)
 */
public class ServerJoinPoint extends AbstractJoinPoint {
    private RpcServer rpcServer;

    public ServerJoinPoint(Controller controller, Request request, RpcServer rpcServer) {
        super(controller, request);
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
        if (request.getRpcMethodInfo().isIncludeController()) {
            Object[] args = new Object[request.getArgs().length + 1];
            args[0] = controller;
            for (int i = 0; i < request.getArgs().length; i++) {
                args[i + 1] = request.getArgs()[i];
            }
            return request.getTargetMethod().invoke(request.getTarget(), args);
        } else {
            return request.getTargetMethod().invoke(request.getTarget(), request.getArgs());
        }
    }
}
