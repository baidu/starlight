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

import com.baidu.brpc.buffer.DynamicCompositeByteBuf;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.channel.BrpcChannel;
import com.baidu.brpc.exceptions.BadSchemaException;
import com.baidu.brpc.exceptions.NotEnoughDataException;
import com.baidu.brpc.exceptions.TooBigDataException;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;

/**
 * rpc协议接口，业务如果想实现一个自定义协议，需要实现该接口。
 * Created by huwenwei on 2017/9/22.
 */
public interface Protocol {

    /**************** 以下5个函数是客户端、服务端都要实现的。 *******************/

    /**
     * create a new request instance
     */
    Request createRequest();

    /**
     * create a new response instance
     */
    Response createResponse();

    /**
     * get a reusable request instance from threadLocal or pool
     * the request instance must be reset before reuse
     */
    Request getRequest();

    /**
     * get a reusable response instance from threadLocal or pool
     * the response instance must be reset before reuse
     */
    Response getResponse();

    /**
     * 客户端/服务端解析请求包成header+body buffer
     * @param in 输入byte buf
     * @return header+body buffer
     * @throws BadSchemaException header格式不对
     * @throws TooBigDataException body太大
     * @throws NotEnoughDataException 可读长度不够，由于粘包拆包问题。
     */
    Object decode(ChannelHandlerContext ctx, DynamicCompositeByteBuf in, boolean isDecodingRequest)
            throws BadSchemaException, TooBigDataException, NotEnoughDataException;

    /**************** 以下4个函数是客户端需要实现的。 *******************/

    /**
     * 客户端序列化请求对象
     * @param request 待发送给服务端的对象
     * @throws Exception 序列化异常
     */
    ByteBuf encodeRequest(Request request) throws Exception;

    /**
     * do something if needed before client send request
     */
    void beforeRequestSent(Request request, RpcClient rpcClient, BrpcChannel channelGroup);

    /**
     * 客户端反序列化rpc响应
     * @param msg header & body的buf
     * @param ctx netty channel context
     * @throws Exception 反序列化异常
     */
    Response decodeResponse(Object msg, ChannelHandlerContext ctx) throws Exception;

    /**
     * 连接被归还入池的时机
     * @return true代表请求发送后立即归还连接，无需等待响应
     */
    boolean returnChannelBeforeResponse();

    /**************** 以下3个函数是服务端需要实现的。 *******************/

    /**
     * 服务端反序列化rpc请求
     * @param packet header & body的buf
     */
    Request decodeRequest(Object packet) throws Exception;

    /**
     * 服务端序列化返回结果。
     * @param response 服务端要返回给客户端的对象
     * @throws Exception 序列化异常
     */
    ByteBuf encodeResponse(Request request, Response response) throws Exception;

    /**
     * do something if needed after server channel writeAndFlush
     * @param channelFuture the return value of writeAndFlush
     */
    void afterResponseSent(Request request, Response response, ChannelFuture channelFuture);

}
