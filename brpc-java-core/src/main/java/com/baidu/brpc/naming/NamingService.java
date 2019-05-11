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
package com.baidu.brpc.naming;

import java.util.List;

import com.baidu.brpc.client.instance.ServiceInstance;

public interface NamingService {
    /**
     * 查询符合条件的已注册数据，与订阅的推模式相对应，这里为拉模式，只返回一次结果。
     *
     * @param subscribeInfo service/group/version info
     * @return 已注册信息列表，可能为空。
     */
    List<ServiceInstance> lookup(SubscribeInfo subscribeInfo);

    /**
     * 订阅符合条件的已注册数据，当有注册数据变更时自动推送.
     *
     * @param listener 变更事件监听器，不允许为空
     */
    void subscribe(SubscribeInfo subscribeInfo, NotifyListener listener);

    /**
     * 取消订阅.
     *
     */
    void unsubscribe(SubscribeInfo subscribeInfo);

    /**
     * 注册数据，比如：提供者地址，消费者地址，路由规则，覆盖规则，等数据。
     *
     * @param registerInfo service/group/version info
     */
    void register(RegisterInfo registerInfo);

    /**
     * 取消注册.
     *
     * @param registerInfo service/group/version info
     */
    void unregister(RegisterInfo registerInfo);
}
