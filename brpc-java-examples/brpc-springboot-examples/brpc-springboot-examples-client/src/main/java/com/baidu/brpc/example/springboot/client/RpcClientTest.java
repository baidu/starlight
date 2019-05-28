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

package com.baidu.brpc.example.springboot.client;

import com.baidu.brpc.example.springboot.api.EchoRequest;
import com.baidu.brpc.example.springboot.api.EchoResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Future;

@SpringBootApplication
@RestController
public class RpcClientTest {
    @Autowired
    private EchoFacade echoFacade;

    public static void main(String[] args) {
        SpringApplication.run(RpcClientTest.class, args);
    }

    @RequestMapping("/echo")
    public String echo() {
        EchoRequest request = new EchoRequest();
        request.setMessage("hello");
        EchoResponse response = echoFacade.echo(request);
        System.out.println(response.getMessage());

        EchoResponse response2 = echoFacade.echo2(request);
        System.out.println(response2.getMessage());

        Future<EchoResponse> future = echoFacade.echo3(request);
        try {
            future.get();
        } catch (Exception ex) {
            // ignore
        }

        return response2.getMessage();
    }
}
