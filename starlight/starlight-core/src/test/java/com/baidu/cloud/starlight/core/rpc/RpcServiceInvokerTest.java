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
 
package com.baidu.cloud.starlight.core.rpc;

import com.baidu.cloud.starlight.api.exception.StarlightRpcException;
import com.baidu.cloud.starlight.api.rpc.RpcService;
import com.baidu.cloud.starlight.core.integrate.model.User;
import com.baidu.cloud.starlight.core.integrate.service.UserService;
import com.baidu.cloud.starlight.core.integrate.service.UserServiceImpl;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import io.netty.util.Timeout;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by liuruisen on 2020/3/23.
 */
public class RpcServiceInvokerTest {

    private RpcServiceInvoker rpcServiceInvoker;

    @Before
    public void before() {
        RpcService rpcService = new RpcService(UserService.class, new UserServiceImpl());
        rpcServiceInvoker = new RpcServiceInvoker(rpcService);
    }

    @Test
    public void getRpcService() {
        assertEquals(rpcServiceInvoker.getRpcService().getServiceName(), UserService.class.getName());
    }

    @Test
    public void invoke() {
        Request request = new RpcRequest();
        request.setMethodName("getUser1");
        request.setServiceName("normal:" + UserService.class.getName() + ":1.0.0");
        request.setProtocolName("brpc");
        request.setParams(new Object[] {1l});

        RpcCallback rpcCallback = new RpcCallback() {
            @Override
            public void addTimeout(Timeout timeout) {}

            @Override
            public Request getRequest() {
                return request;
            }

            @Override
            public void onResponse(Response response) {
                Assert.assertTrue(response.getResult() instanceof User);
                assertEquals(((User) response.getResult()).getUserId(), 1l);
            }

            @Override
            public void onError(Throwable e) {
                Assert.assertTrue(e instanceof StarlightRpcException);
            }
        };
        rpcServiceInvoker.invoke(request, rpcCallback);

        request.setMethodName("getUser");
        rpcServiceInvoker.invoke(request, rpcCallback);

        request.setParams(new Object[] {"1"});
        rpcServiceInvoker.invoke(request, rpcCallback);
    }

    @Test
    public void destroy() {
        rpcServiceInvoker.destroy();
    }

    @Test
    public void init() {
        rpcServiceInvoker.init();
    }

    @Test
    public void convertThrowable() throws NoSuchMethodException {

        Method illArguMethod = this.getClass().getMethod("throwException", Integer.class);
        try {
            illArguMethod.invoke(this, "123"); // IllegalArgumentException
        } catch (Throwable e) {
            StarlightRpcException starlightRpcException = rpcServiceInvoker.convertThrowable(e);
            assertEquals(StarlightRpcException.BIZ_ERROR, starlightRpcException.getCode());
            assertTrue(starlightRpcException.getMessage().contains(IllegalArgumentException.class.getSimpleName()));
        }

        Method illAccMeth = this.getClass().getDeclaredMethod("illegalAccException");
        try {
            illAccMeth.invoke(this); // IllegalAccessExp
        } catch (Throwable e) {
            StarlightRpcException starlightRpcException = rpcServiceInvoker.convertThrowable(e);
            assertEquals(StarlightRpcException.BIZ_ERROR, starlightRpcException.getCode());
            assertTrue(starlightRpcException.getMessage().contains(IllegalAccessException.class.getSimpleName()));
        }

        Method throwErrMeth = this.getClass().getDeclaredMethod("throwError");
        try {
            throwErrMeth.invoke(this); // Error
        } catch (Throwable e) {
            StarlightRpcException starlightRpcException = rpcServiceInvoker.convertThrowable(e);
            assertEquals(StarlightRpcException.BIZ_ERROR, starlightRpcException.getCode());
            assertTrue(starlightRpcException.getMessage().contains(StackOverflowError.class.getSimpleName()));
        }

        Method throwExpMeth = this.getClass().getMethod("throwException", Integer.class);
        try {
            throwExpMeth.invoke(this, 1); // exception
        } catch (Throwable e) {
            StarlightRpcException starlightRpcException = rpcServiceInvoker.convertThrowable(e);
            assertEquals(StarlightRpcException.BIZ_ERROR, starlightRpcException.getCode());
            assertTrue(starlightRpcException.getMessage().contains(Exception.class.getSimpleName()));
            assertTrue(starlightRpcException.getMessage().contains("Exception test"));
        }

        Method throwNpeMeth = this.getClass().getMethod("throwNPE");
        try {
            throwNpeMeth.invoke(this);
        } catch (Throwable e) {
            StarlightRpcException starlightRpcException = rpcServiceInvoker.convertThrowable(e);
            assertEquals(StarlightRpcException.BIZ_ERROR, starlightRpcException.getCode());
            assertTrue(starlightRpcException.getMessage().contains(NullPointerException.class.getSimpleName()));
        }

        StarlightRpcException starlightRpcException = rpcServiceInvoker.convertThrowable(new Exception());
        assertEquals(StarlightRpcException.BIZ_ERROR, starlightRpcException.getCode());
        assertTrue(starlightRpcException.getMessage().contains(Exception.class.getSimpleName()));

        StarlightRpcException starlightRpcException1 =
            rpcServiceInvoker.convertThrowable(new Exception(new Exception("Error1111")));
        assertEquals(StarlightRpcException.BIZ_ERROR, starlightRpcException1.getCode());
        assertTrue(starlightRpcException1.getMessage().contains("Error1111"));
    }

    public void throwException(Integer index) throws Exception {
        throw new Exception("Exception test");
    }

    public void throwNPE() {
        throw new NullPointerException();
    }

    private void illegalAccException() {}

    public void throwError() {
        throw new StackOverflowError();
    }

}