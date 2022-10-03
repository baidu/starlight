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
 
package com.baidu.cloud.starlight.springcloud.shutdown;

import com.baidu.cloud.starlight.api.rpc.StarlightServer;
import com.baidu.cloud.starlight.springcloud.server.register.StarlightRegisterListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

/**
 * Created by liuruisen on 2020/12/3.
 */
public class StarlightGracefullyShutdownLifecycle implements SmartLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(StarlightGracefullyShutdownLifecycle.class);

    private volatile boolean running = false;

    private final StarlightServer starlightServer;

    private final StarlightRegisterListener registerListener;

    public StarlightGracefullyShutdownLifecycle(StarlightServer starlightServer,
        StarlightRegisterListener registerListener) {
        this.starlightServer = starlightServer;
        this.registerListener = registerListener;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        // deregister
        registerListener.deRegister(); // async and catch error
        // shutdown: gracefully or immediate
        starlightServer.destroy(); // catch error
        // unlock and perform next shutdown process
        callback.run();
    }

    @Override
    public void start() {
        this.running = true;
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
