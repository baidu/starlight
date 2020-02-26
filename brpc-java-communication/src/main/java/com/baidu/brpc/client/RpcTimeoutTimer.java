/*
 * Copyright (C) 2019 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.brpc.client;

import com.baidu.brpc.ChannelInfo;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.protocol.Protocol;
import com.baidu.brpc.protocol.Response;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by wanghongfei on 2019-04-18.
 */
public class RpcTimeoutTimer implements TimerTask {
    private static final Logger LOG = LoggerFactory.getLogger(RpcTimeoutTimer.class);

    private ChannelInfo channelInfo;
    private long correlationId;
    private Protocol protocol;

    public RpcTimeoutTimer(
            ChannelInfo channelInfo,
            long correlationId,
            Protocol protocol) {
        this.channelInfo = channelInfo;
        this.correlationId = correlationId;
        this.protocol = protocol;
    }

    @Override
    public void run(Timeout timeout) {
        RpcFuture future = channelInfo.removeRpcFuture(correlationId);
        if (future != null) {
            String ip = future.getChannelInfo().getChannelGroup().getServiceInstance().getIp();
            int port = future.getChannelInfo().getChannelGroup().getServiceInstance().getPort();
            long elapseTime = System.currentTimeMillis() - future.getStartTime();
            String errMsg = String.format("request timeout,correlationId=%d,ip=%s,port=%d,elapse=%dms",
                    correlationId, ip, port, elapseTime);
            LOG.info(errMsg);
            Response response = protocol.createResponse();
            response.setException(new RpcException(RpcException.TIMEOUT_EXCEPTION, errMsg));
            response.setRpcFuture(future);
            future.handleResponse(response);
        }
    }
}
