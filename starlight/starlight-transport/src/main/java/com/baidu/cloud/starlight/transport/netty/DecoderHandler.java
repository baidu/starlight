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
import com.baidu.cloud.starlight.api.rpc.LocalContext;
import com.baidu.cloud.starlight.api.rpc.RpcContext;
import com.baidu.cloud.starlight.api.transport.buffer.DynamicCompositeByteBuf;
import com.baidu.cloud.starlight.api.transport.channel.ChannelAttribute;
import com.baidu.cloud.starlight.api.transport.channel.ChannelSide;
import com.baidu.cloud.starlight.api.transport.channel.RpcChannel;
import com.baidu.cloud.starlight.api.protocol.Protocol;
import com.baidu.cloud.starlight.api.utils.LogUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Set;

/**
 * Protocol decoding in the IO thread Created by liuruisen on 2020/2/3.
 */
public class DecoderHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DecoderHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {

        ChannelAttribute attribute = ctx.channel().attr(RpcChannel.ATTRIBUTE_KEY).get();
        if (attribute == null) {
            throw new TransportException("Netty Channel don't have ChannelAttribute instance, Channel Illegal");
        }

        // set currentThread's contextClassLoader to requestThread's ClassLoader
        // this make Protostuff get the correct Schema bound with ClassLoader
        ClassLoader classLoader = LocalContext.getContext(Constants.LOCAL_CONTEXT_THREAD_CLASSLOADER_KEY)
            .get(ctx.channel().id().asLongText());
        if (classLoader != null && attribute.getRpcChannel().side().equals(ChannelSide.CLIENT)) {
            Thread.currentThread().setContextClassLoader(classLoader);
        } else if (attribute.getRpcChannel().side().equals(ChannelSide.CLIENT)) {
            LOGGER.warn("Class Loader related to channel {} is null, plz check", ctx.channel().id().asLongText());
        }

        RpcChannel rpcChannel = attribute.getRpcChannel();
        String remoteAddress = "";
        try {
            if (rpcChannel.getRemoteAddress() != null) {
                remoteAddress = rpcChannel.getRemoteAddress().getAddress().getHostAddress() + ":"
                    + rpcChannel.getRemoteAddress().getPort();
            }
        } catch (Exception e) {
            LOGGER.warn("Get remote addr from channel failed, remote addr value will be null, cause by: {}",
                e.getMessage());
        }

        long receiveTime = System.currentTimeMillis();
        long msgSize = msg.readableBytes();

        LOGGER.debug("Receive msg from {}, size {}", remoteAddress, msgSize);

        if (msg != null && msg.readableBytes() > 0) {
            attribute.getDynamicByteBuf().addBuffer(msg.retain());
        }

        // protocol decode and fire message
        while (attribute.getDynamicByteBuf().readableBytes() > 0) {
            try {
                // protocol decode
                MsgBase msgBase = protocolDecode(attribute, attribute.getDynamicByteBuf());

                // fire message
                if (msgBase != null) {
                    LogUtils.addLogTimeAttachment(msgBase, Constants.RECEIVE_BYTE_MSG_TIME_KEY, receiveTime);
                    msgBase.getNoneAdditionKv().put(Constants.REMOTE_ADDRESS_KEY, remoteAddress);
                    LogUtils.addLogTimeAttachment(msgBase, Constants.BEFORE_DECODE_HEADER_TIME_KEY, receiveTime);
                    LogUtils.addLogTimeAttachment(msgBase, Constants.DECODE_HEADER_COST,
                        System.currentTimeMillis() - receiveTime);

                    if (msgBase.getAttachmentKv() == null) {
                        msgBase.setAttachmentKv(new HashMap<>());
                    }
                    // fixbug: remote address is illegal in server side, will add remote address to RpcContext
                    try {
                        msgBase.getAttachmentKv().put(RpcContext.REMOTE_HOST_KEY,
                            rpcChannel.getRemoteAddress().getAddress().getHostAddress());
                        msgBase.getAttachmentKv().put(RpcContext.REMOTE_PORT_KEY,
                            rpcChannel.getRemoteAddress().getPort());
                    } catch (Exception e) {
                        LOGGER.debug(
                            "Get remote addr from channel failed, remote addr value will be null, " + "cause by: {}",
                            e.getMessage());
                    }

                    ctx.fireChannelRead(msgBase);
                }

            } catch (CodecException e) { // know and unknow decode failed
                if (e.getCode().equals(CodecException.PROTOCOL_DECODE_NOTENOUGHDATA_EXCEPTION)
                    || e.getCode().equals(CodecException.PROTOCOL_INSUFFICIENT_DATA_EXCEPTION)) { // retry decode
                                                                                                  // next time
                    LOGGER.warn(
                        "Decode header with the byteBuf failed, will retry: side {}, remoteAddr {}, "
                            + "recvMsgTime {}, size {}, cause by {}.",
                        rpcChannel.side(), remoteAddress, receiveTime, msgSize, e.getMessage());
                    break;
                } else { // throw decode exception
                    LOGGER.warn(
                        "Decode header with the byteBuf failed: side {}, remoteAddr {}, "
                            + "recvMsgTime {}, size {}, the last exception is {}.",
                        rpcChannel.side(), remoteAddress, receiveTime, msgSize, e.getMessage());
                    throw e;
                }
            }
        }

    }

    private MsgBase protocolDecode(ChannelAttribute attribute, DynamicCompositeByteBuf byteBuf) throws CodecException {
        // decode know protocol
        try {
            if (attribute.getChannelProtocol() != null
                && !attribute.getChannelProtocol().equals(Constants.UNSPECIFIED_PROTOCOL)) {
                return knowProtocolDecode(attribute.getChannelProtocol(), byteBuf);
            }
        } catch (CodecException e) {
            // protocol not match will retry decode use unknow protocol
            // Insufficient data exception, wait and decoder later
            if (e.getCode().equals(CodecException.PROTOCOL_DECODE_NOTMATCH_EXCEPTION)) {
                attribute.resetChannelProtocol(Constants.UNSPECIFIED_PROTOCOL);
            } else { // protocol decode failed, throw exception
                throw e;
            }
        }

        // decode unKnow protocol, Protocol is not specified
        if (attribute.getChannelProtocol() == null
            || attribute.getChannelProtocol().equals(Constants.UNSPECIFIED_PROTOCOL)) {
            return unKnowProtocolDecode(attribute, byteBuf);
        }

        return null;
    }

    /**
     * Decode message with know protocol May throw CodecException, when decode fail
     *
     * @param protocolName
     * @param byteBuf
     * @return
     * @throws CodecException
     */
    private MsgBase knowProtocolDecode(String protocolName, DynamicCompositeByteBuf byteBuf) throws CodecException {
        Protocol protocol = ExtensionLoader.getInstance(Protocol.class).getExtension(protocolName);
        return protocol.getDecoder().decode(byteBuf);
    }

    /**
     * Try decoding using all protocols
     *
     * @param byteBuf
     * @return
     * @throws CodecException
     */
    private MsgBase unKnowProtocolDecode(ChannelAttribute attribute, DynamicCompositeByteBuf byteBuf)
        throws CodecException {
        MsgBase msgObj = null;
        Set<String> protocols = ExtensionLoader.getInstance(Protocol.class).getSupportedExtensions();
        int protocolNum = 1;
        // all support protocols
        for (String protocolName : protocols) {
            try {
                msgObj = knowProtocolDecode(protocolName, byteBuf);
                if (msgObj != null) {
                    // specified protocol, cache protocol to improve decoding efficiency
                    attribute.resetChannelProtocol(protocolName);
                    break;
                }
            } catch (CodecException e) {
                if (e.getCode().equals(CodecException.PROTOCOL_DECODE_NOTMATCH_EXCEPTION)) { // retry another protocol
                    // LOGGER.debug("Use {} to decode byte, failed {}", protocolName, e.getMessage());
                    if (protocolNum < protocols.size()) {
                        continue;
                    }
                    throw e;
                }

                // match protocol but exception
                // specified protocol, cache protocol to improve decoding efficiency
                attribute.resetChannelProtocol(protocolName);
                LOGGER.debug(
                    "Attempts to use multiple protocols to decode failed. "
                        + "The reason for the last failed attempt is: protocol {}, message {}",
                    protocolName, e.getMessage());
                throw e; // notEnoughData or other decode exception

            } finally {
                protocolNum++;
            }
        }

        return msgObj;
    }

}
