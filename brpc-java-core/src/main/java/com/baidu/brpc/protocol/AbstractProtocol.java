/*
 * Copyright (c) 2018 Baidu, Inc. All Rights Reserved.
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

package com.baidu.brpc.protocol;

import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.channel.BrpcChannel;
import com.baidu.brpc.exceptions.NotEnoughDataException;

import io.netty.channel.ChannelFuture;

/**
 * An abstract protocol used to simplify tcp protocol implementations.
 * Http protocol implementations extended this class need to override most methods,
 * or implements {@link Protocol} interface directly.
 */
public abstract class AbstractProtocol implements Protocol {

    protected static NotEnoughDataException notEnoughDataException = new NotEnoughDataException();

    @Override
    public Request createRequest() {
        // tcp protocol implementation, http protocols should override this method
        return new RpcRequest();
    }

    @Override
    public Response createResponse() {
        // tcp protocol implementation, http protocols should override this method
        return new RpcResponse();
    }

    @Override
    public Request getRequest() {
        // tcp protocol implementation, http protocols should override this method
        Request request = RpcRequest.getRpcRequest();
        request.reset();
        return request;
    }

    @Override
    public Response getResponse() {
        // tcp protocol implementation, http protocols should override this method
        Response response = RpcResponse.getRpcResponse();
        response.reset();
        return response;
    }

    @Override
    public void beforeRequestSent(Request request, RpcClient rpcClient, BrpcChannel channelGroup) {
        // By default, in tcp protocols, there's nothing to to
    }

    @Override
    public boolean returnChannelBeforeResponse() {
        return true;
    }

    @Override
    public void afterResponseSent(Request request, Response response, ChannelFuture channelFuture) {
        // By default, in tcp protocols, there's nothing to to
    }
}
