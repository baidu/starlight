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
 
package com.baidu.cloud.starlight.protocol.http;

import com.baidu.cloud.thirdparty.netty.channel.ChannelHandlerContext;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpObject;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpObjectAggregator;
import com.baidu.cloud.thirdparty.netty.util.ReferenceCountUtil;

import java.util.List;

/**
 * Created by liuruisen on 2020/9/16.
 */
public class HttpRequestResponseAggregator extends HttpObjectAggregator {

    public HttpRequestResponseAggregator(int maxContentLength) {
        super(maxContentLength);
    }

    /**
     * Aggregate http message into full http message by calling
     * {@link HttpObjectAggregator#decode(ChannelHandlerContext, Object, List)}
     * 
     * @param ctx
     * @param msg
     * @param out
     * @throws Exception
     */
    public void aggregate(final ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {

        if (acceptInboundMessage(msg)) {
            try {
                decode(ctx, (HttpObject) msg, out); // aggregate http message
            } finally {
                ReferenceCountUtil.release(msg);
            }
        } else {
            out.add(msg);
        }
    }
}
