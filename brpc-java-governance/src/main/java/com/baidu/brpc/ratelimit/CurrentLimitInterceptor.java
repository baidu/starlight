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

package com.baidu.brpc.ratelimit;

import com.baidu.brpc.interceptor.AbstractInterceptor;
import com.baidu.brpc.protocol.Request;

/**
 * Rpc server current limit interceptor
 * Specific current limiting algorithm leaves to the {@link RateLimiter}
 *
 * @author wangjiayin@baidu.com
 * @since 2018/11/26
 */
public class CurrentLimitInterceptor extends AbstractInterceptor {

    private RateLimiter limiter;

    public CurrentLimitInterceptor(RateLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    public boolean handleRequest(Request request) {
        return limiter.isAllowable(request);
    }

}
