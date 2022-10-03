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
 
package com.baidu.cloud.starlight.api.transport;

import com.baidu.cloud.starlight.api.common.URI;
import com.baidu.cloud.starlight.api.rpc.Processor;

/**
 * Network peer Created by liuruisen on 2019/11/27.
 */
public interface Peer extends GracefullyShutdown {

    void init();

    /**
     * get Uri: IP+Port && Config
     */
    URI getUri();

    /**
     * close and clear resources
     * 
     * @see ClientPeer
     * @see ServerPeer
     */
    void close();

    /**
     * Execute biz work by Processor Processor to handle request {@link Processor}
     */
    void setProcessor(Processor processor);

    /**
     * Get processor
     * 
     * @return
     */
    Processor getProcessor();

    /**
     * Get status of the peer include the status record time
     * 
     * @return
     */
    PeerStatus status();

    /**
     * Update the status of peer and record the statusChangeTime
     * 
     * @param status
     */
    void updateStatus(PeerStatus status);

}
