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
 
package com.baidu.cloud.starlight.core.rpc.proxy;

import com.baidu.cloud.starlight.api.exception.StarlightRpcException;
import com.baidu.cloud.starlight.api.rpc.callback.Callback;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

/**
 * Created by liuruisen on 2021/3/31.
 */
public class JdkInvocationHandlerTest {

    private static final JdkInvocationHandler jdkHandler = new JdkInvocationHandler(AsyncTestService.class, null, null);

    @Test
    public void targetMethod() throws NoSuchMethodException {
        Method echoMethod = TestService.class.getMethod("echo", String.class);
        assertEquals(echoMethod, jdkHandler.targetMethod(echoMethod));

        Class targateClass = AsyncTestService.class;

        Method futureMethod = targateClass.getMethod("echoFuture", String.class);
        Method method = jdkHandler.targetMethod(futureMethod);
        assertNotEquals(futureMethod, method);
        assertEquals(echoMethod, method);

        Method callBackMethod = targateClass.getMethod("echoCallback", String.class, Callback.class);
        Method metho2 = jdkHandler.targetMethod(callBackMethod);
        assertNotEquals(callBackMethod, metho2);
        assertEquals(echoMethod, metho2);

        Method testCallback = targateClass.getMethod("testCallback", String.class, Callback.class);
        try {
            jdkHandler.targetMethod(testCallback);
        } catch (Exception e) {
            assertTrue(e instanceof StarlightRpcException);
        }

    }

    @Test
    public void targetServiceClass() {
        assertEquals(jdkHandler.targetServiceClass(AsyncTestService.class), TestService.class);
        assertEquals(jdkHandler.targetServiceClass(TestService.class), TestService.class);
    }

    @Test
    public void isAsyncCall() throws NoSuchMethodException {
        // TestService
        Method echoMethod = TestService.class.getMethod("echo", String.class);
        assertFalse(jdkHandler.isAsyncCall(echoMethod));

        Method notCallbackMethod = TestService.class.getMethod("notCallback", String.class);
        assertFalse(jdkHandler.isAsyncCall(notCallbackMethod));

        Method notCallback2Method = TestService.class.getMethod("notCallback2", String.class, Callback.class);
        assertFalse(jdkHandler.isAsyncCall(notCallback2Method));

        Method notFutureMethod = TestService.class.getMethod("notFuture", String.class);
        assertFalse(jdkHandler.isAsyncCall(notFutureMethod));

        Method notFuture2Method = TestService.class.getMethod("notFuture2", String.class);
        assertFalse(jdkHandler.isAsyncCall(notFuture2Method));

        // AsyncTestService
        Method callbackMethod = AsyncTestService.class.getMethod("echoCallback", String.class, Callback.class);
        assertTrue(jdkHandler.isAsyncCall(callbackMethod));

        Method futureMethod = AsyncTestService.class.getMethod("echoFuture", String.class);
        assertTrue(jdkHandler.isAsyncCall(futureMethod));

        Method testCallbackMethod = AsyncTestService.class.getMethod("testCallback", String.class, Callback.class);
        assertTrue(jdkHandler.isAsyncCall(testCallbackMethod));

        Method testMethod = AsyncTestService.class.getMethod("test", String.class);
        assertFalse(jdkHandler.isAsyncCall(testMethod));

        Method notAsyncCallbackMethod = AsyncTestService.class.getMethod("notAsyncCallback", String.class);
        assertFalse(jdkHandler.isAsyncCall(notAsyncCallbackMethod));

        Method notAsyncCallbackMethod2 =
            AsyncTestService.class.getMethod("notAsyncCallback", String.class, Callback.class);
        assertFalse(jdkHandler.isAsyncCall(notAsyncCallbackMethod2));

        Method notAsyncFutureMethod = AsyncTestService.class.getMethod("notAsyncFuture", String.class);
        assertFalse(jdkHandler.isAsyncCall(notAsyncFutureMethod));

    }
}