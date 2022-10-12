/*
 * Copyright (c) 2019 Baidu, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package com.baidu.cloud.starlight.transport.channel;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.common.URI;
import com.baidu.cloud.starlight.api.exception.TransportException;
import com.baidu.cloud.starlight.api.transport.channel.RpcChannelGroup;
import com.baidu.cloud.thirdparty.netty.bootstrap.Bootstrap;
import com.baidu.cloud.thirdparty.netty.channel.Channel;
import com.baidu.cloud.thirdparty.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * Created by liuruisen on 2020/2/4.
 */
public abstract class NettyRpcChannelGroup implements RpcChannelGroup {

    protected static final Logger LOGGER = LoggerFactory.getLogger(NettyRpcChannelGroup.class);

    private URI uri;

    private final Bootstrap bootstrap;

    public NettyRpcChannelGroup(URI uri, Bootstrap bootstrap) {
        this.uri = uri;
        this.bootstrap = bootstrap;
    }

    @Override
    public URI getUri() {
        return this.uri;
    }

    /**
     * Connect to the remote server, and return a netty {@link Channel}
     * 
     * @return
     * @throws TransportException
     */
    public synchronized Channel connect() throws TransportException {
        ChannelFuture channelFuture = null;
        try {
            channelFuture = bootstrap.connect(getConnectAddress());
            int connectTimeout = uri.getParameter(Constants.CONNECT_TIMEOUT_KEY, Constants.CONNECT_TIMEOUT_VALUE);
            channelFuture.awaitUninterruptibly(connectTimeout); // wait until connect complete, so we can return Channel
            if (channelFuture.isSuccess()) {
                Channel result = channelFuture.channel();
                LOGGER.info("Create new channel {}, remoteAddress {}", result.id().asLongText(),
                    result.remoteAddress());
                return result;
            } else {
                String causeByMsg =
                    channelFuture.cause() == null ? "Connect timeout" : channelFuture.cause().getMessage();
                LOGGER.info("Create new channel failed, remoteAddress {}, cause by {}", getConnectAddress(),
                    causeByMsg);
                throw new TransportException(TransportException.CONNECT_EXCEPTION, "Connect to url:"
                    + getUri().getHost() + ":" + getUri().getPort() + " failed, " + " cause by : " + causeByMsg);
            }
        } catch (Exception e) {
            if (e instanceof TransportException) {
                throw e;
            }
            if (channelFuture != null /*&& !channelFuture.isSuccess()*/) {
                /*// clean abnormal channel to prevent some unexpected error
                disconnect(channelFuture.channel());*/
                String causeByMsg =
                    channelFuture.cause() == null ? "Connect timeout" : channelFuture.cause().getMessage();
                LOGGER.info("Create new channel failed, remoteAddress {}, cause by {}", getConnectAddress(),
                    causeByMsg);
                throw new TransportException(TransportException.CONNECT_EXCEPTION, "Connect to url:"
                    + getUri().getHost() + ":" + getUri().getPort() + " failed, " + " cause by : " + causeByMsg);
            }
            throw new TransportException(TransportException.CONNECT_EXCEPTION, e.getMessage(), e);
        }
    }

    protected InetSocketAddress getConnectAddress() {
        return new InetSocketAddress(getUri().getHost(), getUri().getPort());
    }
}
