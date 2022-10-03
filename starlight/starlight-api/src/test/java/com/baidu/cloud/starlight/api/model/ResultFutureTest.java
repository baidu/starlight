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
 
package com.baidu.cloud.starlight.api.model;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.exception.StarlightRpcException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by liuruisen on 2020/3/24.
 */
public class ResultFutureTest {

    private Response response;

    @Before
    public void before() {
        response = new RpcResponse();
        response.setResult("111");
        response.setStatus(Constants.SUCCESS_CODE);
    }

    @Test
    public void putResponse() throws ExecutionException, InterruptedException {
        ResultFuture resultFuture = new ResultFuture();
        resultFuture.putResponse(response);
        Assert.assertEquals(resultFuture.get(), "111");
        Assert.assertTrue(resultFuture.isDone());
        Assert.assertTrue(resultFuture.isCancelled());
        Assert.assertFalse(resultFuture.cancel(true));
    }

    @Test
    public void testGet() throws InterruptedException, ExecutionException, TimeoutException {
        ResultFuture resultFuture = new ResultFuture();
        resultFuture.putResponse(response);
        Assert.assertEquals(resultFuture.get(100, TimeUnit.MILLISECONDS), "111");
    }

    @Test
    public void testGetError() throws InterruptedException, ExecutionException, TimeoutException {
        ResultFuture resultFuture = new ResultFuture();
        Response response = new RpcResponse();
        response.setStatus(1000);
        response.setErrorMsg("Error");
        resultFuture.putResponse(response);
        try {
            resultFuture.get(100, TimeUnit.MILLISECONDS);
        } catch (StarlightRpcException e) {
            Assert.assertTrue(e.getCode() == 1000);
        }
    }
}