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
 
package com.baidu.cloud.starlight.api.common;

import com.baidu.cloud.starlight.api.utils.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Created by liuruisen on 2020/3/24.
 */
public class URITest {

    public String baseUriString = "brpc://admin:hello1234@10.20.30.40:20880/context/path?app=mail";

    public URI baseUri = URI.valueOf(baseUriString);

    public enum STAR {
        red, blue
    }

    @Test
    public void test_valueOf_noProtocolAndHost() throws Exception {
        URI uri = URI.valueOf("/context/path?version=1.0.0&app=mail");
        assertNull(uri.getProtocol());
        assertNull(uri.getUsername());
        assertNull(uri.getPassword());
        assertNull(uri.getHost());
        assertEquals(0, uri.getPort());
        assertEquals("context/path", uri.getPath());
        assertEquals(2, uri.getParameters().size());
        assertEquals("1.0.0", uri.getParameter("version"));
        assertEquals("mail", uri.getParameter("app"));

        uri = URI.valueOf("context/path?version=1.0.0&app=mail");
        assertNull(uri.getProtocol());
        assertNull(uri.getUsername());
        assertNull(uri.getPassword());
        assertEquals("context", uri.getHost());
        assertEquals(0, uri.getPort());
        assertEquals("path", uri.getPath());
        assertEquals(2, uri.getParameters().size());
        assertEquals("1.0.0", uri.getParameter("version"));
        assertEquals("mail", uri.getParameter("app"));
    }

    @Test
    public void test_valueOf_noProtocol() throws Exception {
        URI uri = URI.valueOf("10.20.30.40");
        assertNull(uri.getProtocol());
        assertNull(uri.getUsername());
        assertNull(uri.getPassword());
        assertEquals("10.20.30.40", uri.getHost());
        assertEquals(0, uri.getPort());
        assertEquals(null, uri.getPath());
        assertEquals(0, uri.getParameters().size());

        uri = URI.valueOf("10.20.30.40:20880");
        assertNull(uri.getProtocol());
        assertNull(uri.getUsername());
        assertNull(uri.getPassword());
        assertEquals("10.20.30.40", uri.getHost());
        assertEquals(20880, uri.getPort());
        assertEquals(null, uri.getPath());
        assertEquals(0, uri.getParameters().size());

        uri = URI.valueOf("10.20.30.40/context/path");
        assertNull(uri.getProtocol());
        assertNull(uri.getUsername());
        assertNull(uri.getPassword());
        assertEquals("10.20.30.40", uri.getHost());
        assertEquals(0, uri.getPort());
        assertEquals("context/path", uri.getPath());
        assertEquals(0, uri.getParameters().size());

        uri = URI.valueOf("10.20.30.40:20880/context/path");
        assertNull(uri.getProtocol());
        assertNull(uri.getUsername());
        assertNull(uri.getPassword());
        assertEquals("10.20.30.40", uri.getHost());
        assertEquals(20880, uri.getPort());
        assertEquals("context/path", uri.getPath());
        assertEquals(0, uri.getParameters().size());

        uri = URI.valueOf("admin:hello1234@10.20.30.40:20880/context/path?version=1.0.0&app=mail");
        assertNull(uri.getProtocol());
        assertEquals("admin", uri.getUsername());
        assertEquals("hello1234", uri.getPassword());
        assertEquals("10.20.30.40", uri.getHost());
        assertEquals(20880, uri.getPort());
        assertEquals("context/path", uri.getPath());
        assertEquals(2, uri.getParameters().size());
        assertEquals("1.0.0", uri.getParameter("version"));
        assertEquals("mail", uri.getParameter("app"));
    }

    @Test
    public void test_valueOf_noHost() throws Exception {
        URI uri = URI.valueOf("file:///home/user1/router.js");
        assertEquals("file", uri.getProtocol());
        assertNull(uri.getUsername());
        assertNull(uri.getPassword());
        assertNull(uri.getHost());
        assertEquals(0, uri.getPort());
        assertEquals("home/user1/router.js", uri.getPath());
        assertEquals(0, uri.getParameters().size());

        uri = URI.valueOf("file://home/user1/router.js");
        assertEquals("file", uri.getProtocol());
        assertNull(uri.getUsername());
        assertNull(uri.getPassword());
        assertEquals("home", uri.getHost());
        assertEquals(0, uri.getPort());
        assertEquals("user1/router.js", uri.getPath());
        assertEquals(0, uri.getParameters().size());

        uri = URI.valueOf("file:/home/user1/router.js");
        assertEquals("file", uri.getProtocol());
        assertNull(uri.getUsername());
        assertNull(uri.getPassword());
        assertNull(uri.getHost());
        assertEquals(0, uri.getPort());
        assertEquals("home/user1/router.js", uri.getPath());
        assertEquals(0, uri.getParameters().size());

        uri = URI.valueOf("file:///d:/home/user1/router.js");
        assertEquals("file", uri.getProtocol());
        assertNull(uri.getUsername());
        assertNull(uri.getPassword());
        assertNull(uri.getHost());
        assertEquals(0, uri.getPort());
        assertEquals("d:/home/user1/router.js", uri.getPath());
        assertEquals(0, uri.getParameters().size());

        uri = URI.valueOf("file:///home/user1/router.js?p1=v1&p2=v2");
        assertEquals("file", uri.getProtocol());
        assertNull(uri.getUsername());
        assertNull(uri.getPassword());
        assertNull(uri.getHost());
        assertEquals(0, uri.getPort());
        assertEquals("home/user1/router.js", uri.getPath());
        assertEquals(2, uri.getParameters().size());
        Map<String, String> params = new HashMap<String, String>();
        params.put("p1", "v1");
        params.put("p2", "v2");
        assertEquals(params, uri.getParameters());

        uri = URI.valueOf("file:/home/user1/router.js?p1=v1&p2=v2");
        assertEquals("file", uri.getProtocol());
        assertNull(uri.getUsername());
        assertNull(uri.getPassword());
        assertNull(uri.getHost());
        assertEquals(0, uri.getPort());
        assertEquals("home/user1/router.js", uri.getPath());
        assertEquals(2, uri.getParameters().size());
        params = new HashMap<String, String>();
        params.put("p1", "v1");
        params.put("p2", "v2");
        assertEquals(params, uri.getParameters());
    }

    @Test
    public void test_valueOf_WithProtocolHost() throws Exception {
        URI uri = URI.valueOf("brpc://10.20.30.40");
        assertEquals("brpc", uri.getProtocol());
        assertNull(uri.getUsername());
        assertNull(uri.getPassword());
        assertEquals("10.20.30.40", uri.getHost());
        assertEquals(0, uri.getPort());
        assertEquals(null, uri.getPath());
        assertEquals(0, uri.getParameters().size());

        uri = URI.valueOf("brpc://10.20.30.40:20880/context/path");
        assertEquals("brpc", uri.getProtocol());
        assertNull(uri.getUsername());
        assertNull(uri.getPassword());
        assertEquals("10.20.30.40", uri.getHost());
        assertEquals(20880, uri.getPort());
        assertEquals("context/path", uri.getPath());
        assertEquals(0, uri.getParameters().size());

        uri = URI.valueOf("brpc://admin:hello1234@10.20.30.40:20880");
        assertEquals("brpc", uri.getProtocol());
        assertEquals("admin", uri.getUsername());
        assertEquals("hello1234", uri.getPassword());
        assertEquals("10.20.30.40", uri.getHost());
        assertEquals(20880, uri.getPort());
        assertEquals(null, uri.getPath());
        assertEquals(0, uri.getParameters().size());

        uri = URI.valueOf("brpc://admin:hello1234@10.20.30.40:20880?version=1.0.0");
        assertEquals("brpc", uri.getProtocol());
        assertEquals("admin", uri.getUsername());
        assertEquals("hello1234", uri.getPassword());
        assertEquals("10.20.30.40", uri.getHost());
        assertEquals(20880, uri.getPort());
        assertEquals(null, uri.getPath());
        assertEquals(1, uri.getParameters().size());
        assertEquals("1.0.0", uri.getParameter("version"));

        uri = URI.valueOf("brpc://admin:hello1234@10.20.30.40:20880/context/path?version=1.0.0&app=mail");
        assertEquals("brpc", uri.getProtocol());
        assertEquals("admin", uri.getUsername());
        assertEquals("hello1234", uri.getPassword());
        assertEquals("10.20.30.40", uri.getHost());
        assertEquals(20880, uri.getPort());
        assertEquals("context/path", uri.getPath());
        assertEquals(2, uri.getParameters().size());
        assertEquals("1.0.0", uri.getParameter("version"));
        assertEquals("mail", uri.getParameter("app"));

        uri = URI.valueOf("brpc://admin:hello1234@10.20.30.40:20880/context/path?version=1.0.0&app=mail&noValue");
        assertEquals("brpc", uri.getProtocol());
        assertEquals("admin", uri.getUsername());
        assertEquals("hello1234", uri.getPassword());
        assertEquals("10.20.30.40", uri.getHost());
        assertEquals(20880, uri.getPort());
        assertEquals("context/path", uri.getPath());
        assertEquals(3, uri.getParameters().size());
        assertEquals("1.0.0", uri.getParameter("version"));
        assertEquals("mail", uri.getParameter("app"));
        assertEquals("noValue", uri.getParameter("noValue"));
    }

    @Test
    public void test_valueOf_Exception_noProtocol() throws Exception {
        try {
            URI.valueOf("://1.2.3.4:8080/path");
            fail();
        } catch (IllegalStateException expected) {
            assertEquals("uri missing protocol: \"://1.2.3.4:8080/path\"", expected.getMessage());
        }
    }

    @Test
    public void test_getAddress() throws Exception {
        URI uri1 = URI.valueOf("brpc://admin:hello1234@10.20.30.40:20880/context/path?version=1.0.0&app=mail");
        assertEquals("10.20.30.40:20880", uri1.getAddress());
    }

    @Test
    public void test_getAbsolutePath() throws Exception {
        URI uri = new URI.Builder("p1", "1.2.2.2", 33).build();
        assertEquals(null, uri.getAbsolutePath());

        uri = new URI.Builder("file", null, 33).path("/home/user1/route.js").build();
        assertEquals("/home/user1/route.js", uri.getAbsolutePath());
    }

    @Test
    public void test_equals() throws Exception {
        URI uri1 = URI.valueOf("brpc://admin:hello1234@10.20.30.40:20880/context/path?version=1.0.0&app=mail");

        Map<String, String> params = new HashMap<String, String>();
        params.put("version", "1.0.0");
        params.put("app", "mail");
        URI uri2 = new URI.Builder("brpc", "10.20.30.40", 20880).path("context/path").params(params).build();
        assertEquals(uri1, uri2);

        URI uri3 =
            URI.valueOf("brpc://10.95.105.153:3002?executes=50&group=test_dev&interface=" + "demo.v3.HelloWorldService"
                + "&interface.simple=HelloWorldService&registry=default&brccgate.version=1.2.15" + "&version=1.0.0");
        URI uri4 = URI.valueOf("brpc://10.95.105.153:3002?executes=50&group=test_dev&interface=demo.v3.DemoService"
            + "&interface.simple=DemoService&registry=default&brpcgate.version=1.2.15&version=1.0.0");
        System.out.println(uri3.hashCode());
        System.out.println(uri3.hashCode());
        System.out.println(uri3.equals(uri4));
    }

    @Test
    public void test_toString() throws Exception {
        URI uri1 = URI.valueOf("brpc://admin:hello1234@10.20.30.40:20880/context/path?version=1.0.0");
        assertEquals(uri1.toString(), "brpc://10.20.30.40:20880/context/path?version=1.0.0");
    }

    @Test
    public void test_toFullString() throws Exception {
        assertEquals(baseUri.toFullString(), baseUriString);
    }

    @Test
    public void test_toIdentityString() throws Exception {
        assertEquals(baseUri.toIdentityString(), "brpc://admin:hello1234@10.20.30.40:20880/context/path");
    }

    @Test
    public void test_toParameterString() throws Exception {
        URI uri1 = URI.valueOf("brpc://admin:hello1234@10.20.30.40:20880/context/path?app=mail&k1=v1");

        assertEquals(uri1.toParameterString(), "app=mail&k1=v1");
    }

    @Test
    public void test_set_methods() throws Exception {
        URI uri = URI.valueOf("brpc://admin:hello1234@10.20.30.40:20880/context/path?version=1.0.0&app=mail");

        uri = uri.resetHost("host");

        assertEquals("brpc", uri.getProtocol());
        assertEquals("admin", uri.getUsername());
        assertEquals("hello1234", uri.getPassword());
        assertEquals("host", uri.getHost());
        assertEquals(20880, uri.getPort());
        assertEquals("context/path", uri.getPath());
        assertEquals(2, uri.getParameters().size());
        assertEquals("1.0.0", uri.getParameter("version"));
        assertEquals("mail", uri.getParameter("app"));

        uri = uri.resetPort(1);

        assertEquals("brpc", uri.getProtocol());
        assertEquals("admin", uri.getUsername());
        assertEquals("hello1234", uri.getPassword());
        assertEquals("host", uri.getHost());
        assertEquals(1, uri.getPort());
        assertEquals("context/path", uri.getPath());
        assertEquals(2, uri.getParameters().size());
        assertEquals("1.0.0", uri.getParameter("version"));
        assertEquals("mail", uri.getParameter("app"));

        uri = uri.resetPath("path");

        assertEquals("brpc", uri.getProtocol());
        assertEquals("admin", uri.getUsername());
        assertEquals("hello1234", uri.getPassword());
        assertEquals("host", uri.getHost());
        assertEquals(1, uri.getPort());
        assertEquals("path", uri.getPath());
        assertEquals(2, uri.getParameters().size());
        assertEquals("1.0.0", uri.getParameter("version"));
        assertEquals("mail", uri.getParameter("app"));

        uri = uri.resetProtocol("protocol");

        assertEquals("protocol", uri.getProtocol());
        assertEquals("admin", uri.getUsername());
        assertEquals("hello1234", uri.getPassword());
        assertEquals("host", uri.getHost());
        assertEquals(1, uri.getPort());
        assertEquals("path", uri.getPath());
        assertEquals(2, uri.getParameters().size());
        assertEquals("1.0.0", uri.getParameter("version"));
        assertEquals("mail", uri.getParameter("app"));

        uri = uri.resetUsername("username");

        assertEquals("protocol", uri.getProtocol());
        assertEquals("username", uri.getUsername());
        assertEquals("hello1234", uri.getPassword());
        assertEquals("host", uri.getHost());
        assertEquals(1, uri.getPort());
        assertEquals("path", uri.getPath());
        assertEquals(2, uri.getParameters().size());
        assertEquals("1.0.0", uri.getParameter("version"));
        assertEquals("mail", uri.getParameter("app"));

        uri = uri.resetPassword("password");

        assertEquals("protocol", uri.getProtocol());
        assertEquals("username", uri.getUsername());
        assertEquals("password", uri.getPassword());
        assertEquals("host", uri.getHost());
        assertEquals(1, uri.getPort());
        assertEquals("path", uri.getPath());
        assertEquals(2, uri.getParameters().size());
        assertEquals("1.0.0", uri.getParameter("version"));
        assertEquals("mail", uri.getParameter("app"));
    }

    @Test
    public void test_removeParameters() throws Exception {
        URI uri =
            URI.valueOf("brpc://admin:hello1234@10.20.30.40:20880/context/path?version=1.0.0&app=mail&k1=v1&k2=v2");

        uri = uri.removeParameter("version");
        assertEquals("brpc", uri.getProtocol());
        assertEquals("admin", uri.getUsername());
        assertEquals("hello1234", uri.getPassword());
        assertEquals("10.20.30.40", uri.getHost());
        assertEquals(20880, uri.getPort());
        assertEquals("context/path", uri.getPath());
        assertEquals(3, uri.getParameters().size());
        assertEquals("mail", uri.getParameter("app"));
        assertEquals("v1", uri.getParameter("k1"));
        assertEquals("v2", uri.getParameter("k2"));

        uri = URI.valueOf("brpc://admin:hello1234@10.20.30.40:20880/context/path?version=1.0.0&app=mail&k1=v1&k2=v2");
        uri = uri.removeParameters("version", "app");
        assertEquals("brpc", uri.getProtocol());
        assertEquals("admin", uri.getUsername());
        assertEquals("hello1234", uri.getPassword());
        assertEquals("10.20.30.40", uri.getHost());
        assertEquals(20880, uri.getPort());
        assertEquals("context/path", uri.getPath());
        assertEquals(2, uri.getParameters().size());
        assertEquals("v1", uri.getParameter("k1"));
        assertEquals("v2", uri.getParameter("k2"));

        uri = URI.valueOf("brpc://admin:hello1234@10.20.30.40:20880/context/path?version=1.0.0&app=mail&k1=v1&k2=v2");
        uri = uri.removeParameters(Arrays.asList("version", "app"));
        assertEquals("brpc", uri.getProtocol());
        assertEquals("admin", uri.getUsername());
        assertEquals("hello1234", uri.getPassword());
        assertEquals("10.20.30.40", uri.getHost());
        assertEquals(20880, uri.getPort());
        assertEquals("context/path", uri.getPath());
        assertEquals(2, uri.getParameters().size());
        assertEquals("v1", uri.getParameter("k1"));
        assertEquals("v2", uri.getParameter("k2"));
        uri = uri.clearParameters();
        assertEquals("", uri.toParameterString());
    }

    @Test
    public void test_addParameters() throws Exception {
        URI uri = baseUri.addParameters(CollectionUtils.toStringMap("k1", "v1", "k2", "v2"));

        assertEquals("brpc", uri.getProtocol());
        assertEquals("admin", uri.getUsername());
        assertEquals("hello1234", uri.getPassword());
        assertEquals("10.20.30.40", uri.getHost());
        assertEquals(20880, uri.getPort());
        assertEquals("context/path", uri.getPath());
        assertEquals(3, uri.getParameters().size());
        assertEquals("mail", uri.getParameter("app"));
        assertEquals("v1", uri.getParameter("k1"));
        assertEquals("v2", uri.getParameter("k2"));

        uri = baseUri.addParameters("k1", "v1", "k2", "v2", "app", "xxx");

        assertEquals("brpc", uri.getProtocol());
        assertEquals("admin", uri.getUsername());
        assertEquals("hello1234", uri.getPassword());
        assertEquals("10.20.30.40", uri.getHost());
        assertEquals(20880, uri.getPort());
        assertEquals("context/path", uri.getPath());
        assertEquals(3, uri.getParameters().size());
        assertEquals("xxx", uri.getParameter("app"));
        assertEquals("v1", uri.getParameter("k1"));
        assertEquals("v2", uri.getParameter("k2"));

        uri = baseUri.addParametersIfAbsent(CollectionUtils.toStringMap("k1", "v1", "k2", "v2", "app", "xxx"));

        assertEquals("brpc", uri.getProtocol());
        assertEquals("admin", uri.getUsername());
        assertEquals("hello1234", uri.getPassword());
        assertEquals("10.20.30.40", uri.getHost());
        assertEquals(20880, uri.getPort());
        assertEquals("context/path", uri.getPath());
        assertEquals(3, uri.getParameters().size());
        assertEquals("mail", uri.getParameter("app"));
        assertEquals("v1", uri.getParameter("k1"));
        assertEquals("v2", uri.getParameter("k2"));

        uri = baseUri.addParameterString("k1=v1&k2=v2");

        assertEquals("brpc", uri.getProtocol());
        assertEquals("admin", uri.getUsername());
        assertEquals("hello1234", uri.getPassword());
        assertEquals("10.20.30.40", uri.getHost());
        assertEquals(20880, uri.getPort());
        assertEquals("context/path", uri.getPath());
        assertEquals(3, uri.getParameters().size());
        assertEquals("mail", uri.getParameter("app"));
        assertEquals("v1", uri.getParameter("k1"));
        assertEquals("v2", uri.getParameter("k2"));

        uri = new URI.Builder(baseUri).param("app", "xxx").build();

        assertEquals("brpc", uri.getProtocol());
        assertEquals("admin", uri.getUsername());
        assertEquals("hello1234", uri.getPassword());
        assertEquals("10.20.30.40", uri.getHost());
        assertEquals(20880, uri.getPort());
        assertEquals("context/path", uri.getPath());
        assertEquals(1, uri.getParameters().size());
        assertEquals("xxx", uri.getParameter("app"));

        uri = new URI.Builder(baseUri).paramIfAbsent("app", "xxx").build();

        assertEquals("brpc", uri.getProtocol());
        assertEquals("admin", uri.getUsername());
        assertEquals("hello1234", uri.getPassword());
        assertEquals("10.20.30.40", uri.getHost());
        assertEquals(20880, uri.getPort());
        assertEquals("context/path", uri.getPath());
        assertEquals(1, uri.getParameters().size());
        assertEquals("mail", uri.getParameter("app"));
    }

    @Test
    public void test_otherMethod() throws Exception {
        URI uri = new URI.Builder(baseUriString).paramString("group=test&version=1.0&interface=com.baidu.test").build();

        assertEquals("test:com.baidu.test:1.0", uri.getServiceKey());
        assertEquals("com.baidu.test", uri.getServiceName());
        assertEquals(new InetSocketAddress("10.20.30.40", 20880), uri.toInetSocketAddress());
        assertEquals("10.20.30.40", uri.getIp());

    }

    @Test
    public void test_getParameters() throws Exception {
        URI uri = new URI.Builder(baseUriString).param("s", "bad").param("i", 999).param("b", true).param("d", 9.9)
            .param("l", 9999l).param("I", new Integer(998)).param("Ered", STAR.red).param("arr", "ss,ff,nn")
            .username("user").password("userpasswd").params("k1", "v1", "k2", "v2").paramString("k3=v3&k4=v4")
            .param("m.arr", "ss,ff,nn")
            .paramString("m.k3=v33&m.k4=v44&m.i=123&m.l=123&m.d=2.123&m.b=false&m.s=mgood&m.s1=mgood1")
            .paramString("fd=-9.9&fi=-9&fl=-99").paramAndEncoded("china", "[当代").build();
        assertEquals("userpasswd", uri.getPassword());
        assertEquals("brpc", uri.getProtocol());
        assertEquals(999, uri.getParameter("i", 888));
        assertEquals(888, uri.getParameter("i2", 888));
        assertEquals("bad", uri.getParameter("s"));
        assertEquals("bad", uri.getParameter("s", "good"));
        assertEquals("good", uri.getParameter("ss", "good"));
        assertArrayEquals(new String[] {"ss", "ff", "nn"}, uri.getParameter("arr", new String[] {"ss", "uu"}));
        assertArrayEquals(new String[] {"ss", "uu"}, uri.getParameter("strarr2", new String[] {"ss", "uu"}));
        assertEquals(9.9, uri.getParameter("d", 8.8), 0.1);
        assertEquals(8.8, uri.getParameter("d2", 8.8), 0.1);
        assertEquals(9999l, uri.getParameter("l", 8888l));
        assertEquals(8888l, uri.getParameter("l2", 8888l));
        assertEquals("true", uri.getParameter("b"));
        assertEquals(true, uri.getParameter("b", false));
        assertEquals(false, uri.getParameter("bfalse", false));
        assertEquals("998", uri.getParameter("I"));
        assertEquals("red", uri.getParameter("Ered"));
        assertEquals("v1", uri.getParameter("k1"));
        assertEquals("v2", uri.getParameter("k2"));
        assertEquals("v3", uri.getParameter("k3"));
        assertEquals("v4", uri.getParameter("k4"));
        assertEquals("v33", uri.getMethodParameter("m", "k3"));
        assertEquals("v1", uri.getMethodParameter("m", "k1"));
        assertEquals("v44", uri.getMethodParameter("m", "k4"));
        assertEquals(123, uri.getMethodParameter("m", "i", 124));
        assertEquals(123l, uri.getMethodParameter("m", "l", 124l));
        assertEquals(2.123d, uri.getMethodParameter("m", "d", 2.123), 0.1);
        assertEquals(false, uri.getMethodParameter("m", "b", true));
        assertEquals("mgood", uri.getMethodParameter("m", "s", "mgood"));
        assertEquals(true, uri.hasMethodParameter("m", "s1"));
        assertEquals(false, uri.hasMethodParameter(null, null));
        assertEquals(false, uri.hasParameter("s2"));
        assertEquals(1000.0, uri.getPositiveParameter("fd", 1000.0), 0.1);
        assertEquals(1000, uri.getPositiveParameter("fi", 1000));
        assertEquals(1000l, uri.getPositiveParameter("fl", 1000l));
        assertEquals("[当代", uri.getMethodParameterAndDecoded("m", "china"));
        assertEquals("[当代", uri.getMethodParameterAndDecoded("m", "china1", "[当代"));
        assertEquals("[当代", uri.getParameterAndDecoded("china", "china"));
        assertEquals("[当代", uri.getParameterAndDecoded("china"));
    }

    @Test
    public void test_windowAbsolutePathBeginWithSlashIsValid() throws Exception {
        final String osProperty = System.getProperties().getProperty("os.name");
        if (!osProperty.toLowerCase().contains("windows"))
            return;

        File f0 = new File("C:/Windows");
        File f1 = new File("/C:/Windows");

        File f2 = new File("C:\\Windows");
        File f3 = new File("/C:\\Windows");
        File f4 = new File("\\C:\\Windows");

        assertEquals(f0, f1);
        assertEquals(f0, f2);
        assertEquals(f0, f3);
        assertEquals(f0, f4);
    }

    @Test
    public void test_javaNetUrl() throws Exception {
        java.net.URL uri =
            URI.valueOf("http://admin:hello1234@10.20.30.40:20880/context/path?app=mail&version=1.0.0#anchor1")
                .toJavaURL();

        assertEquals("http", uri.getProtocol());
        assertEquals("admin:hello1234", uri.getUserInfo());
        assertEquals("10.20.30.40", uri.getHost());
        assertEquals(20880, uri.getPort());
        assertEquals("/context/path", uri.getPath());
        assertEquals("app=mail&version=1.0.0", uri.getQuery());
        assertEquals("anchor1", uri.getRef());

        assertEquals("admin:hello1234@10.20.30.40:20880", uri.getAuthority());
        assertEquals("/context/path?app=mail&version=1.0.0", uri.getFile());
    }

    @Test
    public void test_builder_param_null() {
        URI.Builder builder = new URI.Builder("brpc", "localhost", 8006);
        builder.param("key", ""); // param string string null
        builder.paramString(""); // paramString blank
        builder.paramAndEncoded("key2", ""); // paramAndEncoded blank
        builder.paramIfAbsent("key3", ""); // paramIfAbsent blank
    }

    @Test
    public void test_uri_illegal() {
        // username not exist but have password
        try {
            URI uri = new URI("brpc", "", "123qwe", "0.0.0.0", 8888, "/context", new HashMap<>());
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }

        // port 0
        URI uri = new URI("brpc", "name", "123qwe", "0.0.0.0", 0, "/context", new HashMap<>());
        Assert.assertEquals(uri.getPort(), 0);
    }

    @Test
    public void encode_decode() {
        String msg1 = URI.encode(null);
        Assert.assertEquals(msg1, "");
        String msg2 = URI.decode(null);
        Assert.assertEquals(msg2, "");
        String encodeMsg = URI.encode("你好啊");
        Assert.assertEquals(URI.decode(encodeMsg), "你好啊");
    }

    @Test
    public void equals() {
        URI uri = new URI("brpc", "name", "123qwe", "0.0.0.0", 8888, "/context", new HashMap<>());

        URI uri2 =
            new URI("brpc", "name", "123qwe", "0.0.0.0", 8888, "/context", Collections.singletonMap("key", "value"));

        Assert.assertTrue(uri.equals(uri2));
    }

}