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

package com.baidu.brpc.thread;

import com.baidu.brpc.client.channel.BootstrapManager;
import com.baidu.brpc.utils.ThreadPool;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.Timer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ShutDownManager {
    private static volatile ShutDownManager clientShutDownManager;

    static {
        // do clean work when jvm shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                log.info("Brpc do clean work...");
                shutdownGlobalThreadPools();
            }
        }));
    }

    public static void shutdownGlobalThreadPools() {
        log.info("invoke shutdownGlobalThreadPools");
        ThreadPool serverWorkThreadPool = ServerWorkThreadPoolInstance.getInstance();

        EpollEventLoopGroup epollBossGroup = ServerAcceptorThreadPoolInstance.getEpollInstance();
        NioEventLoopGroup nioBossGroup = ServerAcceptorThreadPoolInstance.getNioInstance();
        EpollEventLoopGroup epollWorkerGroup = ServerIoThreadPoolInstance.getEpollInstance();
        NioEventLoopGroup nioWorkerGroup = ServerIoThreadPoolInstance.getNioInstance();
        Timer clientHealthCheckerTimer = ClientHealthCheckTimerInstance.getInstance();
        Timer clientTimeOutTimer = TimerInstance.getInstance();

        if (epollBossGroup != null) {
            epollBossGroup.shutdownGracefully();
        }
        if (nioBossGroup != null) {
            nioBossGroup.shutdownGracefully();
        }
        if (epollWorkerGroup != null) {
            epollWorkerGroup.shutdownGracefully();
        }
        if (nioWorkerGroup != null) {
            nioWorkerGroup.shutdownGracefully();
        }
        if (serverWorkThreadPool != null) {
            serverWorkThreadPool.stop();
        }
        BrpcThreadPoolManager.getInstance().stopAll();
        BootstrapManager bootstrapManager = BootstrapManager.getInstance();
        bootstrapManager.removeAll();
        if (clientHealthCheckerTimer != null) {
            clientHealthCheckerTimer.stop();
        }
        if (clientTimeOutTimer != null) {
            clientTimeOutTimer.stop();
        }
    }

    private ShutDownManager() {
    }

    public static ShutDownManager getInstance() {
        if (clientShutDownManager == null) {
            synchronized(ShutDownManager.class) {
                if (clientShutDownManager == null) {
                    clientShutDownManager = new ShutDownManager();
                }
            }
        }
        return clientShutDownManager;
    }
}
