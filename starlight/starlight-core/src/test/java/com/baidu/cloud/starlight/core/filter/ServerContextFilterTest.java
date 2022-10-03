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

import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.rpc.Invoker;
import com.baidu.cloud.starlight.api.rpc.RpcContext;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

/**
 * Created by liuruisen on 2020/9/2.
 */
public class ServerContextFilterTest {

    @Test
    public void filterRequest() {
        Invoker invoker = Mockito.mock(Invoker.class);
        doNothing().when(invoker).invoke(any(), any());
        RpcCallback callback = Mockito.mock(RpcCallback.class);
        Request request = new RpcRequest();
        request.setAttachmentKv(Collections.singletonMap("Key", "Value"));
        ServerContextFilter serverContextFilter = new ServerContextFilter();
        serverContextFilter.filterRequest(invoker, request, callback);
        assertEquals(1, RpcContext.getContext().get().size());
        assertEquals("Value", RpcContext.getContext().get().get("Key"));
    }

    @Test
    public void filterResponse() {
        RpcContext.removeContext();
        RpcContext.getContext().set("Key", "Value");
        assertEquals(1, RpcContext.getContext().get().size());
        ServerContextFilter serverContextFilter = new ServerContextFilter();
        serverContextFilter.filterResponse(null, null);
        assertEquals(0, RpcContext.getContext().get().size());
    }
}