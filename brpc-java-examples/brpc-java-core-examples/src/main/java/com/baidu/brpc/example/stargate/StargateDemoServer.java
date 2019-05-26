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

import com.baidu.brpc.naming.NamingOptions;
import com.baidu.brpc.naming.zookeeper.StargateNamingFactory;
import com.baidu.brpc.protocol.Options;
import com.baidu.brpc.server.RpcServer;
import com.baidu.brpc.server.RpcServerOptions;

public class StargateDemoServer {

    public static void main(String[] args) {
        RpcServerOptions serverOptions = new RpcServerOptions();
        serverOptions.setProtocolType(Options.ProtocolType.PROTOCOL_STARGATE_VALUE);
        serverOptions.setNamingServiceUrl(StargateDemoConstant.namingUrl);

        StargateNamingFactory starGateNamingFactory = new StargateNamingFactory();
        RpcServer rpcServer = new RpcServer(8898, serverOptions, null, starGateNamingFactory);

        StargateDemoService demoService = new StargateDemoServiceImpl();

        NamingOptions namingOptions = new NamingOptions();
        namingOptions.setGroup(StargateDemoConstant.group);
        namingOptions.setVersion(StargateDemoConstant.version);

        rpcServer.registerService(demoService, namingOptions);
        rpcServer.start();

        // make server keep running
        synchronized (StargateDemoServer.class) {
            try {
                StargateDemoServer.class.wait();
            } catch (Throwable e) {
            }
        }
    }

}
