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
 
package com.baidu.cloud.starlight.transport.netty;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.exception.CodecException;
import com.baidu.cloud.starlight.api.exception.TransportException;
import com.baidu.cloud.starlight.api.extension.ExtensionLoader;
import com.baidu.cloud.starlight.api.model.MsgBase;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.protocol.Protocol;
import com.baidu.cloud.starlight.api.protocol.ProtocolEncoder;
import com.baidu.cloud.starlight.api.rpc.LocalContext;
import com.baidu.cloud.starlight.api.transport.channel.ChannelAttribute;
import com.baidu.cloud.starlight.api.transport.channel.ChannelSide;
import com.baidu.cloud.starlight.api.transport.channel.RpcChannel;
import com.baidu.cloud.starlight.api.utils.LogUtils;
import com.baidu.cloud.starlight.api.utils.StringUtils;
import com.baidu.cloud.thirdparty.netty.buffer.ByteBuf;
import com.baidu.cloud.thirdparty.netty.channel.ChannelHandler;
import com.baidu.cloud.thirdparty.netty.channel.ChannelHandlerContext;
import com.baidu.cloud.thirdparty.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Protocol encoding in the IO thread Created by liuruisen on 2020/2/3.
 */
@ChannelHandler.Sharable
public class EncoderHandler extends MessageToByteEncoder<MsgBase> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EncoderHandler.class);

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, MsgBase msgBase, ByteBuf byteBuf)
        throws Exception {

        ChannelAttribute attribute = channelHandlerContext.channel().attr(RpcChannel.ATTRIBUTE_KEY).get();
        if (attribute == null) {
            throw new TransportException("Netty Channel don't have ChannelAttribute instance, Channel Illegal");
        }

        // set currentThread's contextClassLoader to requestThread's ClassLoader
        // this make Protostuff get the correct Schema bound with ClassLoader
        ClassLoader classLoader = LocalContext.getContext(Constants.LOCAL_CONTEXT_THREAD_CLASSLOADER_KEY)
            .get(channelHandlerContext.channel().id().asLongText());
        if (classLoader != null && attribute.getRpcChannel().side().equals(ChannelSide.CLIENT)) {
            Thread.currentThread().setContextClassLoader(classLoader);
        } else if (attribute.getRpcChannel().side().equals(ChannelSide.CLIENT)) {
            LOGGER.error("Class Loader related to channel {} is null, plz check",
                channelHandlerContext.channel().id().asLongText());
        }

        if (msgBase == null) {
            throw new CodecException(TransportException.BODY_NULL_EXCEPTION, "Message is null, cannot encode!");
        }

        // 记录等待io线程执行的时间
        Object timeBeforeIoExec = msgBase.getNoneAdditionKv().get(Constants.BEFORE_IO_THREAD_EXECUTE_TIME_KEY);
        if (timeBeforeIoExec instanceof Long) {
            LogUtils.addLogTimeAttachment(msgBase, Constants.WAIT_FOR_IO_THREAD_COST_KEY,
                System.currentTimeMillis() - ((Long) timeBeforeIoExec));
        }

        String protocolName = msgBase.getProtocolName();
        if (StringUtils.isBlank(protocolName)) {
            throw new CodecException("Cannot encode the message, protocol info is null in the message");
        }

        ProtocolEncoder encoder = ExtensionLoader.getInstance(Protocol.class).getExtension(protocolName).getEncoder();
        // Protocol encode
        ByteBuf encodeResult = null;
        try {
            long beforeEncodeHeaderTime = System.currentTimeMillis();
            LogUtils.addLogTimeAttachment(msgBase, Constants.BEFORE_ENCODE_HEADER_TIME_KEY, beforeEncodeHeaderTime);
            if (msgBase instanceof Request) { // server端感知客户端发送请求的近似时间，用于超时原因推断，notice:不随调用链向下传递
                msgBase.getAttachmentKv().put(Constants.BEFORE_ENCODE_HEADER_TIME_KEY, beforeEncodeHeaderTime);
            }
            encodeResult = encoder.encode(msgBase);
            LogUtils.addLogTimeAttachment(msgBase, Constants.ENCODE_HEADER_COST,
                System.currentTimeMillis() - beforeEncodeHeaderTime);
            if (msgBase instanceof Response) { // server side: the request - response is complete, record acc log
                LogUtils.addLogTimeAttachment(msgBase, Constants.RETURN_RESPONSE_TIME_KEY, System.currentTimeMillis());
                LogUtils.recordAccessLog(((Response) msgBase).getRequest(), (Response) msgBase);
            }
            if (msgBase instanceof Request) { // client side: record time before call server
                LogUtils.addLogTimeAttachment(msgBase, Constants.BEFORE_SERVER_EXECUTE_TIME_KEY,
                    System.currentTimeMillis());
            }
            // NOTICE: 涉及一次内存拷贝，可优化？
            byteBuf.writeBytes(encodeResult);
            LOGGER.debug("Send msg to {}, size {}", channelHandlerContext.channel().remoteAddress(),
                byteBuf.readableBytes());
        } catch (CodecException e) {
            LOGGER.warn("Protocol encode fail, protocol: " + protocolName, e);
            throw e;
        } finally {
            // 此处进行encodeResult release的操作,encodeResult被writeBytes后将无用
            if (encodeResult != null && encodeResult.refCnt() > 0) {
                encodeResult.release();
            }
        }
    }
}
