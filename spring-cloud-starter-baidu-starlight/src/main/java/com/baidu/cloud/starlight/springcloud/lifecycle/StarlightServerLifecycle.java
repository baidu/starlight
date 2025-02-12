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
 
package com.baidu.cloud.starlight.springcloud.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;

import com.baidu.cloud.starlight.api.rpc.StarlightServer;
import com.baidu.cloud.starlight.core.rpc.DefaultStarlightServer;
import com.baidu.cloud.starlight.springcloud.server.register.StarlightRegisterListener;

/**
 * Created by liuruisen on 2020/12/3.
 */

public class StarlightServerLifecycle implements SmartLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(StarlightServerLifecycle.class);

    private volatile boolean running = false;

    private volatile boolean firstStart = true;

    private final StarlightServer starlightServer;

    private final StarlightRegisterListener registerListener;

    private final ApplicationContext applicationContext;

    public StarlightServerLifecycle(StarlightServer starlightServer, StarlightRegisterListener registerListener,
        ApplicationContext applicationContext) {
        this.starlightServer = starlightServer;
        this.registerListener = registerListener;
        this.applicationContext = applicationContext;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void start() {
        LOGGER.info("StarlightServerLifecycle start: thread: {}, loader:{}", Thread.currentThread(),
            Thread.currentThread().getContextClassLoader());

        // Server Restore
        if (!firstStart) {
            if (starlightServer instanceof DefaultStarlightServer defaultStarlightServer) {
                // 如果未使用 Gravity, 例如使用 Consul 注册，则体现为 Restore 时不设置新端口，不支持 CRaC
                defaultStarlightServer.restart();
                defaultStarlightServer.serve();
            } else {
                LOGGER.error("DefaultStarlightServer required!");
            }
        }

        if (firstStart) {
            registerListener.register(applicationContext);
        } else {
            registerListener.restartRegisterExecutor();
            // 同上，未使用 Gravity, 则 Restore 阶段不会重新注册，不支持 CRaC
        }

        this.running = true;
        this.firstStart = false;
    }

    /**
     * stop(Runnable callback); callback 由 Spring 框架传入, 用于通知 Spring 容器该组件已经安全停止
     *
     * @param callback
     */
    @Override
    public void stop(Runnable callback) {
        LOGGER.info("StarlightServerLifecycle stop: thread: {}, loader:{}", Thread.currentThread(),
            Thread.currentThread().getContextClassLoader());

        registerListener.deRegister(); // async and catch error

        starlightServer.destroy(); // catch error

        // deRegister() 是一个异步动作，sleep 若干秒等待其完成
        try {
            LOGGER.info("StarlightServerLifecycle.stop(), wait 10s until operation complete");
            Thread.sleep(10 * 1000);
        } catch (InterruptedException e) {
            LOGGER.info("StarlightServerLifecycle.stop(), InterruptedException", e);
        }

        this.running = false;

        // unlock and perform next shutdown process
        callback.run();
    }

    @Override
    public void stop() {
        throw new UnsupportedOperationException("Stop must not be invoked directly");
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 10;
    }
}
