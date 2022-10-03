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
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.model.ResultFuture;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.model.RpcResponse;
import com.baidu.cloud.starlight.api.exception.StarlightRpcException;
import com.baidu.cloud.starlight.transport.utils.TimerHolder;
import com.baidu.cloud.thirdparty.netty.util.Timeout;
import com.baidu.cloud.thirdparty.netty.util.TimerTask;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by liuruisen on 2020/3/24.
 */
public class FutureCallbackTest {

    private Request request;

    private Response response;

    private Timeout timeout;

    @Before
    public void before() {
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
    public void onResponse() throws ExecutionException, InterruptedException, TimeoutException {
        ResultFuture resultFuture = new ResultFuture();
        FutureCallback futureCallback = new FutureCallback(resultFuture, request);
        futureCallback.addTimeout(timeout);
        futureCallback.onResponse(response);
        Assert.assertEquals(resultFuture.get(), "Test");

        ResultFuture resultFuture2 = new ResultFuture();
        FutureCallback futureCallback2 = new FutureCallback(resultFuture2, request);
        futureCallback2.addTimeout(timeout);
        futureCallback2.onResponse(response);
        Assert.assertEquals(resultFuture.get(10, TimeUnit.MILLISECONDS), "Test");
        futureCallback2.onResponse(response);
    }

    @Test
    public void onError() throws ExecutionException, InterruptedException {
        ResultFuture resultFuture = new ResultFuture();
        FutureCallback futureCallback = new FutureCallback(resultFuture, request);
        futureCallback.addTimeout(timeout);
        futureCallback
            .onError(new StarlightRpcException(StarlightRpcException.INTERNAL_SERVER_ERROR, "Internal server error"));
        try {
            resultFuture.get();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof StarlightRpcException);
            Assert
                .assertTrue(((StarlightRpcException) e).getCode().equals(StarlightRpcException.INTERNAL_SERVER_ERROR));
        }

        futureCallback.onError(new Exception());
    }

    @Test
    public void getRequest() {
        ResultFuture resultFuture = new ResultFuture();
        FutureCallback futureCallback = new FutureCallback(resultFuture, request);
        Assert.assertEquals(futureCallback.getRequest(), request);
    }
}