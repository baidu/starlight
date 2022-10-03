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
 
package com.baidu.cloud.starlight.springcloud.client.cluster.route;

import com.baidu.cloud.starlight.springcloud.client.cluster.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by liuruisen on 2021/10/25.
 */
public abstract class AbstractRouter implements Router {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractRouter.class);

    @Override
    public int compareTo(Router o) {
        if (o == null) {
            return -1;
        }
        return this.getPriority() < o.getPriority() ? 1 : -1;
    }
}
