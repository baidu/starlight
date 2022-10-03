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
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.rpc.Invoker;
import com.baidu.cloud.starlight.api.rpc.RpcContext;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

/**
 * Created by liuruisen on 2020/9/2.
 */
public class ClientContextFilterTest {

    @Test
    public void filterRequest() {
        RpcContext.removeContext();
        RpcContext.getContext().set("Key", "Value");
        Invoker invoker = Mockito.mock(Invoker.class);
        doNothing().when(invoker).invoke(any(), any());
        RpcCallback callback = Mockito.mock(RpcCallback.class);
        Request request = new RpcRequest(1);
        ClientContextFilter clientContextFilter = new ClientContextFilter();
        clientContextFilter.filterRequest(invoker, request, callback);
        assertNotNull(request.getAttachmentKv());
        assertNotNull(request.getAttachmentKv().get(Constants.SESSION_ID_KEY));
        assertNotNull(request.getAttachmentKv().get(Constants.REQUEST_ID_KEY));
        assertEquals("Value", request.getAttachmentKv().get("Key"));
    }

    @Test
    public void filterResponse() {
        RpcContext.removeContext();
        RpcContext.getContext().set("Key", "Value");
        assertEquals(1, RpcContext.getContext().get().size());
        ClientContextFilter clientContextFilter = new ClientContextFilter();
        clientContextFilter.filterResponse(null, new RpcRequest());
        assertEquals(1, RpcContext.getContext().get().size());
    }

}