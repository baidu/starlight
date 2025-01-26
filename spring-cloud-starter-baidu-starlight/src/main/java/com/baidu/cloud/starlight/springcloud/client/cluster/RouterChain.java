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
 
package com.baidu.cloud.starlight.springcloud.client.cluster;

import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.springcloud.common.RouteUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Created by liuruisen on 2021/9/29.
 */
public class RouterChain {
    private static final Logger LOGGER = LoggerFactory.getLogger(RouterChain.class);

    private List<Router> routers;

    private Router noneRouter;

    public RouterChain(List<Router> routers, Router noneRouter) {
        if (routers != null && routers.size() > 1) {
            Collections.sort(routers);
        }
        this.routers = routers;
        this.noneRouter = noneRouter;
    }

    /**
     * 具有兜底的机制
     * 
     * @param requestContext
     * @return
     */
    public Cluster route(Request requestContext) {
        if (routers == null || routers.size() == 0) {
            return noneRoute(requestContext);
        }

        for (Router router : routers) {
            Cluster cluster = router.route(requestContext);
            if (cluster != null) {
                return cluster;
            }
        }
        return noneRoute(requestContext);
    }

    /**
     * 降级路由执行逻辑， 一定能match到Cluster
     *
     * @param requestContext
     * @return
     */
    public Cluster noneRoute(Request request) {
        LOGGER.info("[NONE_ROUTE] Request matched none route: req {}, routeClass {}", RouteUtils.reqMsg(request),
            noneRouter.getClass().getSimpleName());
        return noneRouter.route(request);
    }
}
