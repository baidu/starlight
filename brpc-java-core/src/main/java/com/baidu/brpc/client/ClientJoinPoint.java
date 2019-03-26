package com.baidu.brpc.client;

import com.baidu.brpc.ChannelInfo;
import com.baidu.brpc.client.channel.BrpcChannelGroup;
import com.baidu.brpc.exceptions.RpcException;
import com.baidu.brpc.interceptor.AbstractJoinPoint;
import com.baidu.brpc.interceptor.Interceptor;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import com.baidu.brpc.protocol.RpcContext;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * JoinPoint implementation for client
 * @author Li Yuanxin(liyuanxin999@163.com)
 */
public class ClientJoinPoint extends AbstractJoinPoint {
    private static final Logger LOG = LoggerFactory.getLogger(ClientJoinPoint.class);

    private RpcClient rpcClient;
    private RpcFuture rpcFuture;
    private boolean isFinalTry;
    private Channel channel;
    private ChannelInfo channelInfo;
    private BrpcChannelGroup channelGroup;

    public ClientJoinPoint(Request request, RpcClient rpcClient, boolean isFinalTry,
                           RpcFuture rpcFuture, Channel channel, ChannelInfo channelInfo,
                           BrpcChannelGroup channelGroup) {
        super(request);
        Validate.notNull(rpcClient, "rpcClient cannot be null");
        Validate.notNull(rpcFuture, "rpcFuture cannot be null");
        Validate.notNull(channel, "channel cannot be null");
        Validate.notNull(channelInfo, "channelInfo cannot be null");
        Validate.notNull(channelGroup, "channelGroup cannot be null");

        this.rpcClient = rpcClient;
        this.rpcFuture = rpcFuture;
        this.isFinalTry = isFinalTry;
        this.channel = channel;
        this.channelInfo = channelInfo;
        this.channelGroup = channelGroup;
    }

    @Override
    protected List<Interceptor> getInterceptors() {
        return rpcClient.getInterceptors();
    }

    @Override
    protected Object internalProceed() throws Exception {
        final RpcContext rpcContext = RpcContext.getContext();

        // add request to RpcFuture and add timeout task
        final long readTimeout = rpcClient.getRpcClientOptions().getReadTimeoutMillis();
        Timeout timeout = rpcClient.getTimeoutTimer().newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                boolean isSyncAndFinalRequest = isSyncAndFinalRequest();

                RpcFuture rpcFuture;
                if (isSyncAndFinalRequest) {
                    // get and remove
                    rpcFuture = channelInfo.removeRpcFuture(request.getLogId());

                } else {
                    // get only
                    rpcFuture = channelInfo.getRpcFuture(request.getLogId());
                }

                if (rpcFuture != null) {
                    String ip = rpcFuture.getChannelInfo().getChannelGroup().getIp();
                    int port = rpcFuture.getChannelInfo().getChannelGroup().getPort();
                    long elapseTime = System.currentTimeMillis() - rpcFuture.getStartTime();
                    String errMsg = String.format("request timeout,logId=%d,ip=%s,port=%d,elapse=%dms",
                            request.getLogId(), ip, port, elapseTime);
                    LOG.info(errMsg);
                    Response response = rpcClient.getProtocol().createResponse();
                    response.setException(new RpcException(RpcException.TIMEOUT_EXCEPTION, errMsg));

                    if (isSyncAndFinalRequest) {
                        rpcFuture.handleResponse(response);
                    } else {
                        rpcFuture.processConnection(response);
                    }
                }
            }

            private boolean isSyncAndFinalRequest() {
                return rpcFuture.isAsync() || rpcContext.getChannel() != null || isFinalTry;
            }

        }, readTimeout, TimeUnit.MILLISECONDS);

        // set the missing parameters
        rpcFuture.setTimeout(timeout);


        channelInfo.setLogId(rpcFuture.getLogId());

        try {
            // netty在发送完请求后会release，
            // 所以这里要先retain，防止在重试时，refCnt变成0
            request.addRefCnt();
            ByteBuf byteBuf = rpcClient.getProtocol().encodeRequest(request);
            ChannelFuture sendFuture = channel.writeAndFlush(byteBuf);
            sendFuture.awaitUninterruptibly(rpcClient.getRpcClientOptions().getWriteTimeoutMillis());
            if (!sendFuture.isSuccess()) {

                List<BrpcChannelGroup> unHealthyInstances = new ArrayList<BrpcChannelGroup>(1);
                unHealthyInstances.add(channelGroup);
                rpcClient.getEndPointProcessor().updateUnHealthyInstances(unHealthyInstances);

                if (!(sendFuture.cause() instanceof ClosedChannelException)) {
                    LOG.warn("send request failed, channelActive={}, ex=",
                            channel.isActive(), sendFuture.cause());
                }
                String errMsg = String.format("send request failed, channelActive=%b, ex=%s",
                        channel.isActive(), sendFuture.cause().getMessage());
                throw new RpcException(RpcException.NETWORK_EXCEPTION, errMsg);
            }
        } catch (Exception ex) {
            channelInfo.handleRequestFail(rpcClient.getRpcClientOptions().getChannelType());
            timeout.cancel();
            if (ex instanceof RpcException) {
                throw ex;
            } else {
                throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, ex.getMessage());
            }
        }

        // 立即归还channel
        channelInfo.handleRequestSuccess();

        if (rpcFuture.isAsync()) {
            // async: return
            return rpcFuture;
        } else {
            // sync: wait until complete or timeout
            return rpcFuture.get(rpcClient.getRpcClientOptions().getReadTimeoutMillis(), TimeUnit.MILLISECONDS);
        }
    }
}
