package com.baidu.brpc.server.push;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.brpc.RpcContext;
import com.baidu.brpc.server.ChannelManager;

import io.netty.channel.Channel;
import io.netty.util.Attribute;

public class ClientManagerImpl implements ClientManager {

    private static final Logger LOG = LoggerFactory.getLogger(ClientManagerImpl.class);

    public Response registerClient(String clientName) {

        LOG.info("invoke registerClient::clientName = [{}]", clientName);

        RpcContext context = RpcContext.getContext();
        Channel channel = context.getChannel();
        Validate.notNull(channel, "rpc context channel cannot be null");
        Attribute<String> clientInfo = channel.attr(PushChannelContextHolder.CLIENTNAME_KEY);
        clientInfo.set(clientName);
        ChannelManager.getInstance().putChannel(clientName, channel);
        LOG.info("finished save client Name , channel:" + channel.remoteAddress().toString());
        return Response.success();
    }
}
