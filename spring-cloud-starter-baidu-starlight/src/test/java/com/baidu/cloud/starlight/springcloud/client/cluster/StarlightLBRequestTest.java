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

import com.baidu.cloud.starlight.api.model.ResultFuture;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.rpc.StarlightClient;
import com.baidu.cloud.starlight.core.rpc.callback.FutureCallback;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

/**
 * Created by liuruisen on 2020/3/25.
 */
public class StarlightLBRequestTest {

    @Test
    public void apply() {
        StarlightClient starlightClient = Mockito.mock(StarlightClient.class);
        doNothing().when(starlightClient).request(any(), any());
        StarlightLBRequest starlightLBRequest = new StarlightLBRequest(starlightClient, new RpcRequest(),
            new FutureCallback(new ResultFuture(), new RpcRequest()));
        try {
            starlightLBRequest.apply(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}