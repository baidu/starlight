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
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.rpc.ClientInvoker;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.api.transport.ClientPeer;
import com.baidu.cloud.starlight.core.rpc.RpcClientInvoker;
import io.netty.util.Timeout;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by liuruisen on 2021/3/22.
 */
public class GenericFilterTest {

    @Test
    public void filterRequest() {

        Request request = new RpcRequest();
        request.setMethodName("filterRequest");
        request.setServiceName(this.getClass().getName());
        Map<String, Object> kv = new HashMap<>();
        kv.put(Constants.IS_GENERIC_KEY, true);
        request.setAttachmentKv(kv);

        GenericFilter genericFilter = new GenericFilter();
        genericFilter.filterRequest(new ClientInvoker() {
            @Override
            public ClientPeer getClientPeer() {
                return null;
            }

            @Override
            public ServiceConfig getServiceConfig() {
                return null;
            }

            @Override
            public void invoke(Request request, RpcCallback callback) {

            }
        }, request, new RpcCallback() {
            @Override
            public void addTimeout(Timeout timeout) {

            }

            @Override
            public Request getRequest() {
                return null;
            }

            @Override
            public void onResponse(Response response) {

            }

            @Override
            public void onError(Throwable e) {

            }
        });

        assertNull(request.getAttachmentKv().get(Constants.IS_GENERIC_KEY));
    }
}