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

import com.baidu.brpc.exceptions.TooBigDataException;
import com.baidu.brpc.buffer.DynamicCompositeByteBuf;
import com.baidu.brpc.exceptions.BadSchemaException;
import com.baidu.brpc.exceptions.NotEnoughDataException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

/**
 * rpc协议接口，业务如果想实现一个自定义协议，需要实现该接口。
 * Created by huwenwei on 2017/9/22.
 */
public interface Protocol {

    /**
     * 客户端/服务端解析请求包成header+body buffer
     * @param in 输入byte buf
     * @return header+body buffer
     * @throws BadSchemaException header格式不对
     * @throws TooBigDataException body太大
     * @throws NotEnoughDataException 可读长度不够，由于粘包拆包问题。
     */
    Object decode(DynamicCompositeByteBuf in)
            throws BadSchemaException, TooBigDataException, NotEnoughDataException;

    /**************** 以下3个函数是TCP客户端需要实现的。 *******************/
    /**
     * 客户端序列化请求对象
     * @param rpcRequest 待发送给服务端的对象
     * @throws Exception 序列化异常
     */
    ByteBuf encodeRequest(RpcRequest rpcRequest) throws Exception;

    /**
     * 客户端反序列化rpc响应
     * @param packet header & body的buf
     * @param ctx netty channel context
     * @throws Exception 反序列化异常
     */
    RpcResponse decodeResponse(Object packet, ChannelHandlerContext ctx) throws Exception;

    /**
     * 连接被归还入池的时机
     * @return true代表请求发送后立即归还连接，无需等待响应
     */
    boolean returnChannelBeforeResponse();


    /**************** 以下2个函数是TCP服务端需要实现的。 *******************/
    /**
     * 服务端反序列化rpc请求
     * @param packet header & body的buf
     * @return 输出对象
     */
    void decodeRequest(Object packet, RpcRequest rpcRequest) throws Exception;

    /**
     * 服务端序列化返回结果。
     * @param rpcResponse 服务端要返回给客户端的对象
     * @throws Exception 序列化异常
     */
    ByteBuf encodeResponse(RpcResponse rpcResponse) throws Exception;


    /**************** 以下2个函数是HTTP客户端需要实现的。 *******************/
    /**
     * 根据rpc request构建http request
     * @param rpcRequest
     * @return
     * @throws Exception
     */
    FullHttpRequest encodeHttpRequest(RpcRequest rpcRequest) throws Exception;

    /**
     * 根据http response构建rpc response
     * @param httpResponse
     * @return
     */
    RpcResponse decodeHttpResponse(FullHttpResponse httpResponse, ChannelHandlerContext ctx);


    /**************** 以下2个函数是HTTP服务端需要实现的。 *******************/
    /**
     * 根据http request生成rpc request
     * @param httpRequest
     * @return
     */
    void decodeHttpRequest(FullHttpRequest httpRequest, RpcRequest rpcRequest);

    /**
     * 根据rpc response生成http response
     * @param rpcResponse
     * @return
     */
    FullHttpResponse encodeHttpResponse(RpcRequest rpcRequest, RpcResponse rpcResponse);
}
