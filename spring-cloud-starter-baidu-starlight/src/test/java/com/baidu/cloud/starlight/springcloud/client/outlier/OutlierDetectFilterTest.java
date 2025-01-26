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
 
package com.baidu.cloud.starlight.springcloud.client.outlier;

import com.baidu.cloud.starlight.api.common.URI;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.model.RpcResponse;
import com.baidu.cloud.starlight.api.rpc.config.TransportConfig;
import com.baidu.cloud.starlight.api.transport.PeerStatus;
import com.baidu.cloud.starlight.core.rpc.SingleStarlightClient;
import com.baidu.cloud.starlight.core.statistics.FixedTimeWindowStats;
import com.baidu.cloud.starlight.core.statistics.StarlightStatistics;
import com.baidu.cloud.starlight.core.statistics.StarlightStatsManager;
import com.baidu.cloud.starlight.springcloud.client.cluster.SingleStarlightClientManager;
import com.baidu.cloud.starlight.springcloud.common.ApplicationContextUtils;
import com.baidu.cloud.starlight.springcloud.common.SpringCloudConstants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by liuruisen on 2021/4/25.
 */
@RunWith(MockitoJUnitRunner.class)
public class OutlierDetectFilterTest {

    @Before
    public void before() {
        SingleStarlightClientManager singleStarlightClientManager = SingleStarlightClientManager.getInstance();
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        ApplicationContextUtils applicationContextUtils = new ApplicationContextUtils();
        applicationContextUtils.setApplicationContext(applicationContext);
        when(applicationContext.getBean(SingleStarlightClientManager.class)).thenReturn(singleStarlightClientManager);
    }

    @Test
    public void filterResponse() {

        SingleStarlightClientManager clientManager =
            ApplicationContextUtils.getBeanByType(SingleStarlightClientManager.class);
        clientManager.getOrCreateSingleClient("localhost", 8888, new TransportConfig());
        SingleStarlightClient singleStarlightClient = clientManager.getSingleClient("localhost", 8888);

        Response response = new RpcResponse();
        Request request = new RpcRequest();

        OutlierDetectFilter outlierDetectFilter = new OutlierDetectFilter();

        // remote uri is null
        outlierDetectFilter.filterResponse(response, request);
        assertEquals(PeerStatus.Status.ACTIVE, singleStarlightClient.getStatus().getStatus());

        URI.Builder builder = new URI.Builder("brpc", "localhost", 8888);
        URI remoteURI = builder.build();
        request.setRemoteURI(remoteURI);

        // outlier enabled false
        outlierDetectFilter.filterResponse(response, request);
        assertEquals(PeerStatus.Status.ACTIVE, singleStarlightClient.getStatus().getStatus());

        // statistics is null
        remoteURI = remoteURI.addParameters(SpringCloudConstants.OUTLIER_DETECT_ENABLED_KEY, String.valueOf(true));
        request.setRemoteURI(remoteURI);
        outlierDetectFilter.filterResponse(response, request);
        assertEquals(PeerStatus.Status.ACTIVE, singleStarlightClient.getStatus().getStatus());

        // outlier stats is null
        StarlightStatsManager.getOrCreateStats(remoteURI);
        outlierDetectFilter.filterResponse(response, request);
        assertEquals(PeerStatus.Status.ACTIVE, singleStarlightClient.getStatus().getStatus());

        // not reach outlier min request
        remoteURI =
            remoteURI.addParameters(SpringCloudConstants.OUTLIER_DETECT_MINI_REQUEST_NUM_KEY, String.valueOf(3));
        request.setRemoteURI(remoteURI);
        StarlightStatistics starlightStatistics = StarlightStatsManager.getStats(remoteURI);
        starlightStatistics.registerStats(SpringCloudConstants.OUTLIER_STATS_KEY, new FixedTimeWindowStats(100));
        starlightStatistics.record(request, response);
        outlierDetectFilter.filterResponse(response, request);
        assertEquals(PeerStatus.Status.ACTIVE, singleStarlightClient.getStatus().getStatus());

        // not reach fail percent
        starlightStatistics.record(request, response);
        starlightStatistics.record(request, response);
        outlierDetectFilter.filterResponse(response, request);
        assertEquals(PeerStatus.Status.ACTIVE, singleStarlightClient.getStatus().getStatus());

        // outlier
        remoteURI =
            remoteURI.addParameters(SpringCloudConstants.OUTLIER_DETECT_FAIL_PERCENT_THRESHOLD_KEY, String.valueOf(10));
        request.setRemoteURI(remoteURI);
        response.setStatus(1004);
        starlightStatistics.record(request, response);
        starlightStatistics.record(request, response);
        starlightStatistics.record(request, response);
        starlightStatistics.record(request, response);
        outlierDetectFilter.filterResponse(response, request);
        assertEquals(PeerStatus.Status.OUTLIER, singleStarlightClient.getStatus().getStatus());

    }
}