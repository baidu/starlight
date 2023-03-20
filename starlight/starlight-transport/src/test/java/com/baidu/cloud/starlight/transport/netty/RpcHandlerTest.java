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
import com.baidu.cloud.starlight.api.common.URI;
import com.baidu.cloud.starlight.api.exception.TransportException;
import com.baidu.cloud.starlight.api.model.MsgBase;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.model.RpcResponse;
import com.baidu.cloud.starlight.api.rpc.Processor;
import com.baidu.cloud.starlight.api.rpc.RpcService;
import com.baidu.cloud.starlight.api.rpc.ServiceRegistry;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.api.rpc.threadpool.ThreadPoolFactory;
import com.baidu.cloud.starlight.api.transport.channel.ChannelAttribute;
import com.baidu.cloud.starlight.api.transport.channel.ChannelSide;
import com.baidu.cloud.starlight.transport.channel.LongRpcChannel;
import com.baidu.cloud.starlight.api.transport.channel.RpcChannel;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import static org.mockito.Mockito.doNothing;

/**
 * Created by liuruisen on 2020/3/20.
 */
public class RpcHandlerTest {

    private RpcRequest rpcRequest;

    private RpcResponse rpcResponse;

    @Before
    public void init() {
        Map<String, Object> kvMap = new HashMap<>();
        kvMap.put(Constants.TRACE_ID_KEY, 123l);
        kvMap.put(Constants.SPAN_ID_KEY, 2l);
        kvMap.put(Constants.PARENT_SPAN_ID_KEY, 1l);
        kvMap.put("Key1", "Value1");

        rpcRequest = new RpcRequest();
        rpcRequest.setMethodName("init");
        rpcRequest.setServiceConfig(new ServiceConfig());
        rpcRequest.setServiceClass(this.getClass());
        rpcRequest.setParams(new Object[] {"Test"});
        rpcRequest.setParamsTypes(new Class[] {String.class});
        rpcRequest.setProtocolName("brpc");
        rpcRequest.setAttachmentKv(kvMap);

        rpcResponse = new RpcResponse();
        rpcResponse.setResult("Test");
        rpcResponse.setStatus(Constants.SUCCESS_CODE);
        rpcResponse.setErrorMsg("");
        rpcResponse.setProtocolName("brpc");
        rpcResponse.setAttachmentKv(kvMap);
    }

    @Test
    public void clientHandler() {
        URI.Builder builder = new URI.Builder("brpc", "localhost", 8006);

        Processor processor = new Processor() {
            @Override
            public ServiceRegistry getRegistry() {
                return null;
            }

            @Override
            public void process(MsgBase msgBase, RpcChannel context) {

            }

            @Override
            public void close() {

            }

            @Override
            public void setThreadPoolFactory(ThreadPoolFactory threadPoolFactory) {

            }

            @Override
            public Integer waitTaskCount(String serviceKey) {
                return 0;
            }

            @Override
            public Integer processingCount(String serviceKey) {
                return 0;
            }

            @Override
            public Long completeCount(String serviceKey) {
                return 0l;
            }

            @Override
            public Integer allWaitTaskCount() {
                return 0;
            }
        };
        processor.setThreadPoolFactory(new ThreadPoolFactory() {
            @Override
            public ThreadPoolExecutor getThreadPool(RpcService rpcService) {
                return null;
            }

            @Override
            public ThreadPoolExecutor defaultThreadPool() {
                return null;
            }

            @Override
            public void initDefaultThreadPool(URI uri, String threadPrefix) {}

            @Override
            public void close() {}
        });
        NettyClient nettyClient = new NettyClient(builder.build());
        nettyClient.setProcessor(processor);
        EmbeddedChannel channel = new EmbeddedChannel(new RpcHandler(nettyClient));
        LongRpcChannel rpcChannel = new LongRpcChannel(channel, ChannelSide.CLIENT);

        channel.attr(RpcChannel.ATTRIBUTE_KEY).set(new ChannelAttribute(rpcChannel));
        channel.writeInbound(rpcResponse);
        Assert.assertTrue(channel.readInbound() == null);
        channel.finish();
    }

    @Test
    public void serverHandler() {
        URI.Builder builder = new URI.Builder("brpc", "localhost", 8006);

        Processor processor = Mockito.mock(Processor.class);
        doNothing().when(processor).process(ArgumentMatchers.any(), ArgumentMatchers.any());
        NettyServer nettyServer = new NettyServer(builder.build());
        nettyServer.setProcessor(processor);
        EmbeddedChannel channel = new EmbeddedChannel(new RpcHandler(nettyServer));
        try {
            channel.writeInbound(rpcRequest);
        } catch (Exception e) {
            Assert.assertTrue(e instanceof TransportException);
        }

        LongRpcChannel rpcChannel = new LongRpcChannel(channel, ChannelSide.CLIENT);
        channel.attr(RpcChannel.ATTRIBUTE_KEY).set(new ChannelAttribute(rpcChannel));
        channel.writeInbound(rpcRequest);
        Assert.assertTrue(channel.readInbound() == null);
        channel.finish();
    }
}