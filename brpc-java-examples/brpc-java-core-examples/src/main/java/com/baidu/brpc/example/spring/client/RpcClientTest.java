/*
 * Copyright (c) 2018 Baidu, Inc. All Rights Reserved.
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

package com.baidu.brpc.example.spring.client;

import com.baidu.brpc.example.spring.api.EchoRequest;
import com.baidu.brpc.example.spring.api.EchoResponse;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.Future;

public class RpcClientTest {
    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                "classpath:applicationContext.client.xml");
        context.start();

        EchoFacade facade = context.getBean("echoFacadeImpl", EchoFacade.class);
        EchoRequest request = new EchoRequest();
        request.setMessage("hello");
        EchoResponse response = facade.echo(request);
        System.out.println(response.getMessage());

        EchoResponse response2 = facade.echo2(request);
        System.out.println(response2.getMessage());

        Future<EchoResponse> future = facade.echo3(request);
        try {
            future.get();
        } catch (Exception ex) {
            // ignore
        }
    }
}
