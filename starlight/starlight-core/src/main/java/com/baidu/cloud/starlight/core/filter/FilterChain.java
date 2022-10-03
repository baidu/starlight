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
 
package com.baidu.cloud.starlight.core.filter;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.filter.Filter;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.rpc.ClientInvoker;
import com.baidu.cloud.starlight.api.rpc.RpcService;
import com.baidu.cloud.starlight.api.rpc.ServiceInvoker;
import com.baidu.cloud.starlight.core.rpc.callback.FilterCallback;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.api.utils.StringUtils;
import com.baidu.cloud.starlight.api.extension.ExtensionLoader;
import com.baidu.cloud.starlight.api.transport.ClientPeer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * FilterChain 责任链机制 Created by liuruisen on 2019/12/8.
 */
public final class FilterChain {

    /**
     * Build ClientInvoker chain
     * 
     * @param clientInvoker
     * @param filterNames split by ","
     * @return
     */
    public static ClientInvoker buildClientChainInvoker(final ClientInvoker clientInvoker, final String filterNames) {
        // SPI get Client Filters
        List<Filter> filters = getFilters(filterNames);
        // build filter chain ClientInvoker
        if (filters == null || filters.size() == 0) {
            return clientInvoker;
        }
        ClientInvoker resultInvoker = clientInvoker;
        Collections.reverse(filters);
        for (Filter filter : filters) {
            final ClientInvoker next = resultInvoker;
            resultInvoker = new ClientInvoker() {
                @Override
                public ClientPeer getClientPeer() {
                    return clientInvoker.getClientPeer();
                }

                @Override
                public ServiceConfig getServiceConfig() {
                    return clientInvoker.getServiceConfig();
                }

                @Override
                public void invoke(Request request, RpcCallback callback) {
                    filter.filterRequest(next, request, new FilterCallback(callback, filter, request));
                }

                @Override
                public void init() {
                    // do nothing
                }

                @Override
                public void destroy() {
                    clientInvoker.destroy();
                }
            };
        }
        return resultInvoker;
    }

    /**
     * Build ServerInvoker chain
     * 
     * @param serverInvoker
     * @param filterNames split by ","
     * @return
     */
    public static ServiceInvoker buildServerChainInvoker(final ServiceInvoker serverInvoker, final String filterNames) {
        // SPI get Server Filters
        List<Filter> filters = getFilters(filterNames);

        // build filter chain ServerInvoker
        if (filters == null || filters.size() == 0) {
            return serverInvoker;
        }
        ServiceInvoker resultInvoker = serverInvoker;
        Collections.reverse(filters);
        for (Filter filter : filters) {
            final ServiceInvoker next = resultInvoker;
            resultInvoker = new ServiceInvoker() {
                @Override
                public RpcService getRpcService() {
                    return serverInvoker.getRpcService();
                }

                @Override
                public void invoke(Request request, RpcCallback callback) {
                    filter.filterRequest(next, request, new FilterCallback(callback, filter, request));
                }

                @Override
                public void init() {
                    // do nothing
                }

                @Override
                public void destroy() {
                    serverInvoker.destroy();
                }
            };
        }
        return resultInvoker;
    }

    private static List<Filter> getFilters(final String filterNames) {
        if (StringUtils.isBlank(filterNames)) {
            return null;
        }
        List<Filter> filters = new ArrayList<>();
        String[] filterNameList = filterNames.split(Constants.FILTER_NAME_SPLIT_KEY);
        for (String filterName : filterNameList) {
            if (StringUtils.isBlank(filterName)) {
                continue;
            }
            filters.add(ExtensionLoader.getInstance(Filter.class).getExtension(filterName.trim()));
        }
        return filters;
    }
}
