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
 
package com.baidu.cloud.starlight.transport.utils;

import com.baidu.cloud.starlight.api.rpc.threadpool.NamedThreadFactory;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;

/**
 * Created by liuruisen on 2020/2/13.
 */
public class TimerHolder {

    /*
     * static { Runtime.getRuntime().addShutdownHook(new Thread() {
     * 
     * @Override public void run() { DEFAULT_TIMER.stop(); } }); }
     */

    private static final Timer DEFAULT_TIMER = new HashedWheelTimer(new NamedThreadFactory("Timer"));

    private TimerHolder() {

    }

    public static Timer getTimer() {
        return DEFAULT_TIMER;
    }
}
