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
 
package com.baidu.cloud.starlight.springcloud.server.register.gravity;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by liuruisen on 2020/8/13.
 */
public class GravityRegistrationTest {

    @Test
    public void getServiceId() {
        GravityRegistration registration = new GravityRegistration();
        registration.setServiceId("rpc-provider");
        assertEquals("rpc-provider", registration.getServiceId());
    }

    @Test
    public void getHost() {
        GravityRegistration registration = new GravityRegistration();
        registration.setHost("localhost");
        assertEquals("localhost", registration.getHost());
    }

    @Test
    public void getPort() {
        GravityRegistration registration = new GravityRegistration();
        registration.setPort(8888);
        assertEquals(8888, registration.getPort());
    }

    @Test
    public void isSecure() {
        GravityRegistration registration = new GravityRegistration();
        assertFalse(registration.isSecure());
    }

    @Test
    public void getUri() {
        GravityRegistration registration = new GravityRegistration();
        registration.setSchema("rpc");
        registration.setPort(8888);
        registration.setHost("127.0.0.1");
        assertTrue(registration.getUri().toASCIIString().contains("rpc://127.0.0.1:8888"));
    }

    @Test
    public void getMetadata() {
        GravityRegistration registration = new GravityRegistration();
        registration.setMetadata(Collections.singletonMap("key", "val"));
        assertTrue(registration.getMetadata().size() == 1);
        assertEquals(registration.getMetadata().get("key"), "val");

    }

    @Test
    public void getScheme() {
        GravityRegistration registration = new GravityRegistration();
        registration.setSchema("rpc");
        assertEquals("rpc", registration.getScheme());
    }
}