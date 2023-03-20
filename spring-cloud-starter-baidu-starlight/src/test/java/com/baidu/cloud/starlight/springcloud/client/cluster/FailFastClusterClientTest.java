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

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.common.URI;
import com.baidu.cloud.starlight.api.exception.StarlightRpcException;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.model.ResultFuture;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.model.RpcResponse;
import com.baidu.cloud.starlight.api.rpc.callback.RpcCallback;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import com.baidu.cloud.starlight.core.rpc.SingleStarlightClient;
import com.baidu.cloud.starlight.core.rpc.callback.FutureCallback;
import com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants;
import com.baidu.cloud.starlight.springcloud.server.service.TestService;
import com.baidu.cloud.starlight.transport.utils.TimerHolder;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

/**
 * Created by liuruisen on 2020/3/25.
 */
public class FailFastClusterClientTest extends AbstractClusterClientTest {

    @Test
    public void init() {
        URI.Builder uriBuilder = new URI.Builder(Constants.UNSPECIFIED_PROTOCOL, "localhost", 8888);
        SingleStarlightClient starlightClient = Mockito.mock(SingleStarlightClient.class);
        doReturn(uriBuilder.build()).when(starlightClient).remoteURI();

        FailFastClusterClient clusterClient = new FailFastClusterClient("rpc-provider", properties, loadBalancer,
            discoveryClient, clientManager, configuration, routeProperties);
        clusterClient = Mockito.spy(clusterClient);
        doReturn(starlightClient).when(clusterClient).initSingleClient(anyString(), anyInt());

        clusterClient.init();
    }

    @Test
    public void initEmptyInstance() throws NoSuchFieldException, IllegalAccessException {
        DiscoveryClient discoveryClient = Mockito.mock(DiscoveryClient.class);
        doReturn(null).when(discoveryClient).getInstances(anyString());

        clientManager.destroyAll();
        FailFastClusterClient clusterClient = new FailFastClusterClient("rpc-provider", properties, loadBalancer,
            discoveryClient, clientManager, configuration, routeProperties);
        clusterClient.init();
        Map<String, SingleStarlightClient> clientMap = clientManager.allSingleClients();
        assertEquals(clientMap.size(), 0);
    }

    @Test
    public void refer() throws NoSuchFieldException, IllegalAccessException {
        FailFastClusterClient clusterClient = new FailFastClusterClient("rpc-provider", properties, loadBalancer,
            discoveryClient, clientManager, configuration, routeProperties);

        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setProtocol("brpc");
        serviceConfig.setCompressType("none");
        serviceConfig.setFilters("");
        clusterClient.refer(TestService.class, serviceConfig);
        // refer twice
        clusterClient.refer(TestService.class, serviceConfig);

        Field field = clusterClient.getClass().getSuperclass().getDeclaredField("serviceConfigs");
        field.setAccessible(true);
        Map<Class<?>, ServiceConfig> clientMap = (Map<Class<?>, ServiceConfig>) field.get(clusterClient);

        Assert.assertNotNull(clientMap);
        assertEquals(clientMap.size(), 1);
        assertEquals(clientMap.get(TestService.class), serviceConfig);
    }

    @Test
    public void request() throws IOException {
        SingleStarlightClient starlightClient = Mockito.mock(SingleStarlightClient.class);

        FailFastClusterClient clusterClient = new FailFastClusterClient("rpc-provider", properties, loadBalancer,
            discoveryClient, clientManager, configuration, routeProperties);
        ServiceConfig serviceConfig = new ServiceConfig();
        clusterClient.refer(FailFastClusterClient.class, serviceConfig);
        clusterClient = Mockito.spy(clusterClient);
        doReturn(starlightClient).when(clusterClient).initSingleClient(anyString(), anyInt());
        clusterClient.init();

        Request request = new RpcRequest();
        request.setServiceClass(FailFastClusterClient.class);
        request.setServiceConfig(serviceConfig);
        ResultFuture resultFuture = new ResultFuture();
        RpcCallback rpcCallback = new FutureCallback(resultFuture, request);
        // return null
        doReturn(serviceInstance).when(loadBalancerClient).choose(any());
        doReturn(null).when(loadBalancerClient).execute(any(), any(), any());
        clusterClient.request(request, rpcCallback);

        // exception request
        doThrow(new IOException("exception")).when(loadBalancerClient).execute(any(), any(), any());
        clusterClient.request(request, rpcCallback);
        try {
            resultFuture.get();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof StarlightRpcException);
        }
    }

    @Test
    public void requestWithEmptyInstance() throws Exception {
        LoadBalancerClient loadBalancerClient = Mockito.mock(LoadBalancerClient.class);
        doReturn(null).when(loadBalancerClient).choose(anyString());

        clientManager.destroyAll();

        FailFastClusterClient clusterClient = new FailFastClusterClient("rpc-provider", properties, loadBalancer,
            discoveryClient, clientManager, configuration, routeProperties);
        clusterClient.init();
        ServiceConfig serviceConfig = new ServiceConfig();
        clusterClient.refer(FailFastClusterClient.class, serviceConfig);

        Request request = new RpcRequest();
        request.setServiceClass(FailFastClusterClient.class);
        request.setServiceConfig(serviceConfig);
        ResultFuture resultFuture = new ResultFuture();
        RpcCallback rpcCallback = new FutureCallback(resultFuture, request);
        // request without instance
        try {
            clusterClient.request(request, rpcCallback);
        } catch (Exception e) {
            Assert.assertTrue(e instanceof StarlightRpcException);
            assertEquals(SpringCloudConstants.NO_INSTANCE_ERROR_CODE, ((StarlightRpcException) e).getCode());
        }

        doReturn(serviceInstance).when(loadBalancerClient).choose(anyString());
        clusterClient = Mockito.spy(clusterClient);
        SingleStarlightClient starlightClient = Mockito.mock(SingleStarlightClient.class);
        doReturn(starlightClient).when(clusterClient).initSingleClient(anyString(), anyInt());

        Map<String, SingleStarlightClient> clientMap = clientManager.allSingleClients();

        assertEquals(1, clientMap.size());
    }

    @Test
    public void destroy() throws NoSuchFieldException, IllegalAccessException {
        SingleStarlightClient starlightClient = Mockito.mock(SingleStarlightClient.class);

        FailFastClusterClient clusterClient = new FailFastClusterClient("rpc-provider", properties, loadBalancer,
            discoveryClient, clientManager, configuration, routeProperties);
        clusterClient = Mockito.spy(clusterClient);
        doReturn(starlightClient).when(clusterClient).initSingleClient(anyString(), anyInt());
        clusterClient.init();

        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setProtocol("brpc");
        serviceConfig.setCompressType("none");
        serviceConfig.setFilters("");
        clusterClient.refer(TestService.class, serviceConfig);

        Field field2 = clusterClient.getClass().getSuperclass().getSuperclass().getDeclaredField("serviceConfigs");
        field2.setAccessible(true);
        Map<Class<?>, ServiceConfig> serviceConfigMap = (Map<Class<?>, ServiceConfig>) field2.get(clusterClient);
        Assert.assertNotNull(serviceConfigMap);
        assertEquals(serviceConfigMap.size(), 1);
        assertEquals(serviceConfigMap.get(TestService.class), serviceConfig);

        clusterClient.destroy();

        Assert.assertNotNull(serviceConfigMap);
        assertEquals(serviceConfigMap.size(), 0);
    }

    @Test
    public void destroyWithoutInit() throws NoSuchFieldException, IllegalAccessException {
        FailFastClusterClient clusterClient = new FailFastClusterClient("rpc-provider", properties, loadBalancer,
            discoveryClient, clientManager, configuration, routeProperties);
        clusterClient.destroy();
        Field field2 = clusterClient.getClass().getSuperclass().getDeclaredField("serviceConfigs");
        field2.setAccessible(true);
        Map<Class<?>, ServiceConfig> serviceConfigMap = (Map<Class<?>, ServiceConfig>) field2.get(clusterClient);
        assertEquals(serviceConfigMap.size(), 0);
    }

    @Test
    public void initSingleClient() throws IOException, NoSuchFieldException, IllegalAccessException {
        FailFastClusterClient clusterClient = new FailFastClusterClient("rpc-provider", properties, loadBalancer,
            discoveryClient, clientManager, configuration, routeProperties);
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setProtocol("brpc");
        serviceConfig.setCompressType("none");
        serviceConfig.setFilters("");
        clusterClient.refer(TestService.class, serviceConfig);
        // refer twice
        clusterClient.refer(TestService.class, serviceConfig);

        ServerSocket server = new ServerSocket(8899);
        SingleStarlightClient starlightClient = clusterClient.initSingleClient("localhost", 8899);
        Field field = starlightClient.getClass().getDeclaredField("uri");
        field.setAccessible(true);
        URI uri = (URI) field.get(starlightClient);
        Assert.assertNotNull(uri);
        Assert.assertTrue(uri.getPort() == 8899);

        starlightClient.destroy();
        server.close();
    }

    @Test
    public void failFastCallback() throws ExecutionException, InterruptedException {
        FailFastClusterClient clusterClient = new FailFastClusterClient("rpc-provider", properties, loadBalancer,
            discoveryClient, clientManager, configuration, routeProperties);
        ResultFuture resultFuture = new ResultFuture();
        Request request = new RpcRequest();
        RpcCallback rpcCallback = new FutureCallback(resultFuture, request);

        FailFastClusterClient.FailFastClusterCallback failFastClusterCallback =
            clusterClient.new FailFastClusterCallback(rpcCallback);

        Response response = new RpcResponse();
        response.setStatus(Constants.SUCCESS_CODE);
        response.setResult("Hello");

        failFastClusterCallback.addTimeout(TimerHolder.getTimer().newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {

            }
        }, 10, TimeUnit.SECONDS));

        failFastClusterCallback.onResponse(response);
        String result = (String) resultFuture.get();
        assertEquals(result, "Hello");

        assertEquals(failFastClusterCallback.getRequest(), request);
    }

}