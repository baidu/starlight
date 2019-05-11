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
package com.baidu.brpc.example.stargate;

import com.baidu.brpc.RpcContext;
import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.naming.NamingOptions;
import com.baidu.brpc.naming.zookeeper.StargateNamingFactory;
import com.baidu.brpc.protocol.Options;
import com.baidu.brpc.utils.GsonUtils;

public class StargateDemoClient {

    public static void main(String[] args) {
        // 需要声明特殊的 Stargate 注册工厂
        StargateNamingFactory starGateNamingFactory = new StargateNamingFactory();

        RpcClientOptions options = new RpcClientOptions();
        // Stargate 协议需要强指定协议类型，不可使用BRPC协议解析器
        options.setProtocolType(Options.ProtocolType.PROTOCOL_STARGATE_VALUE);
        options.setReadTimeoutMillis(1000);
        options.setWriteTimeoutMillis(1000);
        RpcClient rpcClient = new RpcClient(StargateDemoConstant.namingUrl, options, null, starGateNamingFactory);

        NamingOptions namingOptions = new NamingOptions();
        namingOptions.setGroup(StargateDemoConstant.group);
        namingOptions.setVersion(StargateDemoConstant.version);

        StargateDemoService proxy = BrpcProxy.getProxy(rpcClient, StargateDemoService.class, namingOptions);

        for (int i = 0, times = 10; i < times; i++) {
            RpcContext rpcContext = RpcContext.getContext();
            rpcContext.reset();
            rpcContext.setRequestKvAttachment("key", "value");
            StargateDemoReqDto reqDto = new StargateDemoReqDto();
            reqDto.setId(1000L);
            reqDto.setName("test");
            StargateDemoResDto call = proxy.call(reqDto);
            System.out.println(GsonUtils.toJson(call));
            if (rpcContext.getResponseKvAttachment() != null) {
                System.out.println(rpcContext.getResponseKvAttachment().get("resKey"));
            }
            System.out.println();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        rpcClient.stop();
    }
}
