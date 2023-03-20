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
 
package com.baidu.cloud.starlight.springcloud.client.ribbon;

import com.baidu.cloud.starlight.api.rpc.threadpool.NamedThreadFactory;
import com.baidu.cloud.starlight.springcloud.client.cluster.SingleStarlightClientManager;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import com.netflix.loadbalancer.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;

import java.util.List;
import java.util.Map;

/**
 * Ribbon service list filter defined by Starlight. Support filtering server list peer request, pay attention to the
 * time-consuming execution of filters. Execute in ascending order of {@link Ordered} value.
 *
 * Created by liuruisen on 2021/4/22.
 */
public interface StarlightServerListFilter<T extends Server> extends Ordered {

    Logger LOGGER = LoggerFactory.getLogger(StarlightServerListFilter.class);

    /**
     * Timer used to execute recover tasks, such as clean task
     */
    Timer SERVER_LIST_FILTER_TIMER = new HashedWheelTimer(new NamedThreadFactory("ServerListRecoverTimer"));

    /**
     * Filter origin server list to get more stable server list.
     * 
     * @param originList
     * @return
     */
    List<T> getFilteredList(List<T> originList);

    /**
     * Server list filter timer tasks, such as clean up tasks or recover tasks
     * 
     * @return
     */
    Map<String, Timeout> getServerListFilterTasks();

    /**
     * Submit the server list filter timer task
     * 
     * @param server
     * @param execDelay
     */
    void submitTimerTask(T server, Integer execDelay);

    /**
     * Destroy
     */
    void destroy();

    /**
     * FIXME 改造为ClientManagerAware的形式
     * 
     * @return
     */
    SingleStarlightClientManager getSingleClientManager();
}
