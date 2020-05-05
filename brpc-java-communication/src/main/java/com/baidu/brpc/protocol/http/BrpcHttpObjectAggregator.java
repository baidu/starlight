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
package com.baidu.brpc.protocol.http;

import com.baidu.brpc.exceptions.RpcException;

import java.util.List;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.util.ReferenceCountUtil;

/**
 * Migrate from netty {@link HttpObjectAggregator}
 *
 * @author wangjiayin@baidu.com
 * @since 2019-01-07
 */
public class BrpcHttpObjectAggregator extends HttpObjectAggregator {

    public BrpcHttpObjectAggregator(int maxContentLength) {
        super(maxContentLength);
    }

    public void aggregate(final ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
        if (acceptInboundMessage(msg)) {
            try {
                decode(ctx, (HttpObject) msg, out);
            } finally {
                ReferenceCountUtil.release(msg);
            }
        } else {
            out.add(msg);
        }
    }

    /**
     * Abort aggregation and release underlying resources
     * <p>
     * FIXME find a better way to release memory
     */
    public void abort() {
        try {
            handlerRemoved(null);
        } catch (Exception e) {
            throw new RpcException(e);
        }
    }

}
