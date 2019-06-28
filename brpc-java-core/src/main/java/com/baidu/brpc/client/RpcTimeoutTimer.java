/*
 * Copyright (C) 2019 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.brpc.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.brpc.ChannelInfo;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.protocol.Response;
import com.baidu.brpc.server.RpcServer;

import io.netty.util.Timeout;
import io.netty.util.TimerTask;

/**
 * Created by wanghongfei on 2019-04-18.
 */
public class RpcTimeoutTimer implements TimerTask {
    private static final Logger LOG = LoggerFactory.getLogger(RpcTimeoutTimer.class);

    private ChannelInfo channelInfo;
    private long logId;
    private RpcClient rpcClient;
    private RpcServer rpcServer;

    public RpcTimeoutTimer(
            ChannelInfo channelInfo,
            long logId,
            RpcClient rpcClient) {
        this.channelInfo = channelInfo;
        this.logId = logId;
        this.rpcClient = rpcClient;
    }

    public RpcTimeoutTimer(
            ChannelInfo channelInfo,
            long logId,
            RpcServer rpcServer) {
        this.channelInfo = channelInfo;
        this.logId = logId;
        this.rpcServer = rpcServer;
    }

    @Override
    public void run(Timeout timeout) {
        RpcFuture future = channelInfo.removeRpcFuture(logId);

        if (future != null) {
            String ip = future.getChannelInfo().getChannelGroup().getServiceInstance().getIp();
            int port = future.getChannelInfo().getChannelGroup().getServiceInstance().getPort();
            long elapseTime = System.currentTimeMillis() - future.getStartTime();
            String errMsg = String.format("request timeout,logId=%d,ip=%s,port=%d,elapse=%dms",
                    logId, ip, port, elapseTime);
            LOG.info(errMsg);
            Response response = rpcClient != null ? rpcClient.getProtocol().createResponse() :
                    rpcServer.getProtocol().createResponse();
            response.setException(new RpcException(RpcException.TIMEOUT_EXCEPTION, errMsg));

            future.handleResponse(response);
        }
    }
}
