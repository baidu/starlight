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
 
package com.baidu.cloud.starlight.core.rpc.callback;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.exception.StarlightRpcException;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.model.RpcResponse;
import com.baidu.cloud.starlight.api.rpc.callback.Callback;
import com.baidu.cloud.starlight.transport.utils.TimerHolder;
import com.baidu.cloud.thirdparty.netty.util.Timeout;
import com.baidu.cloud.thirdparty.netty.util.TimerTask;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * Created by liuruisen on 2020/3/24.
 */
public class BizWrapCallbackTest {

    private Callback callback;

    private Request request;

    private Response response;

    private Timeout timeout;

    @Before
    public void before() {
        callback = new Callback() {
            @Override
            public void onResponse(Object response) {
                System.out.println(response);
                Assert.assertTrue(response.equals("Test"));
            }

            @Override
            public void onError(Throwable e) {
                Assert.assertTrue(e instanceof StarlightRpcException);
                Assert.assertTrue(((StarlightRpcException) e).getCode() == StarlightRpcException.INTERNAL_SERVER_ERROR);
            }
        };

        timeout = TimerHolder.getTimer().newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                System.out.println("Timeout");
            }
        }, 3, TimeUnit.SECONDS);

        request = new RpcRequest();
        response = new RpcResponse();
        response.setStatus(Constants.SUCCESS_CODE);
        response.setResult("Test");
    }

    @Test
    public void onResponse() {
        BizWrapCallback bizWrapCallback = new BizWrapCallback(callback, request);
        bizWrapCallback.addTimeout(timeout);
        bizWrapCallback.onResponse(response);
        bizWrapCallback.onResponse(response);
    }

    @Test
    public void onError() {
        BizWrapCallback bizWrapCallback = new BizWrapCallback(callback, request);
        bizWrapCallback.addTimeout(timeout);
        bizWrapCallback
            .onError(new StarlightRpcException(StarlightRpcException.INTERNAL_SERVER_ERROR, "Internal server error"));
    }

    @Test
    public void getRequest() {
        BizWrapCallback bizWrapCallback = new BizWrapCallback(callback, request);
        Assert.assertEquals(bizWrapCallback.getRequest(), request);
    }
}