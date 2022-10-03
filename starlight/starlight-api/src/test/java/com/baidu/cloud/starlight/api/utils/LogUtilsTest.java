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
 
package com.baidu.cloud.starlight.api.utils;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.model.RpcRequest;
import com.baidu.cloud.starlight.api.model.RpcResponse;
import com.baidu.cloud.starlight.api.rpc.config.ServiceConfig;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by liuruisen on 2021/1/8.
 */
public class LogUtilsTest {

    @Test
    public void addLogTimeAttachment() {
        RpcResponse response = new RpcResponse();
        assertEquals(0, response.getNoneAdditionKv().size());
        LogUtils.addLogTimeAttachment(response, Constants.BEFORE_ENCODE_BODY_TIME_KEY, System.currentTimeMillis());
        assertEquals(1, response.getNoneAdditionKv().size());

        RpcRequest request = new RpcRequest();
        assertEquals(0, request.getNoneAdditionKv().size());
        LogUtils.addLogTimeAttachment(request, Constants.BEFORE_ENCODE_BODY_TIME_KEY, System.currentTimeMillis());
        assertEquals(1, request.getNoneAdditionKv().size());
    }

    @Test
    public void recordAccessLog() {
        Request request = new RpcRequest();
        LogUtils.addLogTimeAttachment(request, Constants.BEFORE_ENCODE_BODY_TIME_KEY, System.currentTimeMillis());
        LogUtils.addLogTimeAttachment(request, Constants.ENCODE_BODY_COST, 12);
        LogUtils.addLogTimeAttachment(request, Constants.BEFORE_THREAD_EXECUTE_TIME_KEY, System.currentTimeMillis());

        Response response = new RpcResponse();

        LogUtils.recordAccessLog(request, response);

        request.setServiceClass(this.getClass());
        LogUtils.recordAccessLog(request, response);

        ServiceConfig serviceConfig = new ServiceConfig();
        request.setServiceConfig(serviceConfig);
        LogUtils.recordAccessLog(request, response);
    }

    @Test
    public void recordRequestLog() {

        Request request = new RpcRequest();
        LogUtils.addLogTimeAttachment(request, Constants.BEFORE_ENCODE_BODY_TIME_KEY, System.currentTimeMillis());
        LogUtils.addLogTimeAttachment(request, Constants.ENCODE_BODY_COST, 12);
        LogUtils.addLogTimeAttachment(request, Constants.BEFORE_THREAD_EXECUTE_TIME_KEY, System.currentTimeMillis());

        Response response = new RpcResponse();

        LogUtils.recordRequestLog(request, response);

        request.setServiceClass(this.getClass());
        LogUtils.recordRequestLog(request, response);

        ServiceConfig serviceConfig = new ServiceConfig();
        request.setServiceConfig(serviceConfig);
        LogUtils.recordRequestLog(request, response);

    }

    @Test
    public void timestampToString() {

        long time = System.currentTimeMillis();

        System.out.println(LogUtils.timestampToString(time));

        assertNotNull(LogUtils.timestampToString(time));
    }

    @Test
    public void simpleServiceName() {
        String serviceId = "starlight-provider";

        assertEquals("starlight-provider", LogUtils.simpleServiceName(serviceId));

        String serviceId2 = "starlight.";
        assertEquals(serviceId2, LogUtils.simpleServiceName(serviceId2));

        String serviceName = this.getClass().getName();
        assertEquals(this.getClass().getSimpleName(), LogUtils.simpleServiceName(serviceName));

    }

    @Test
    public void parseTrace() {
        Request request = new RpcRequest();
        request.setAttachmentKv(new HashMap<>());

        Map<String, String> spanTraceMap = LogUtils.parseTraceIdSpanId(request);
        assertNull(spanTraceMap.get(LogUtils.TCID));
        assertNull(spanTraceMap.get(LogUtils.SPID));

        String info = "tcid=6d4eecaf-5fcf-11eb-9e5c-3d7693ec528c&spid=6d4eecae-5fcf-11eb-9e5c-3d7693ec528c&";
        request.getAttachmentKv().put(LogUtils.PROBE_SPAN_CONTEXT_KEY, info);
        spanTraceMap = LogUtils.parseTraceIdSpanId(request);
        assertNotNull(spanTraceMap.get(LogUtils.TCID));
        assertNotNull(spanTraceMap.get(LogUtils.SPID));

        String traceInfo2 = "tcid=6d4eecaf-5fcf-11eb-9e5c-3d7693ec528c&spid=6d4eecae-5fcf-11eb-9e5c-3d7693ec528c"
            + "&user_id=4434331950&";
        request.getAttachmentKv().put(LogUtils.PROBE_SPAN_CONTEXT_KEY, traceInfo2);
        spanTraceMap = LogUtils.parseTraceIdSpanId(request);
        assertNotNull(spanTraceMap.get(LogUtils.TCID));
        assertNotNull(spanTraceMap.get(LogUtils.SPID));

        String traceInfo3 = "tcid=6d4eecaf-5fcf-11eb-9e5c-3d7693ec528c";
        request.getAttachmentKv().put(LogUtils.PROBE_SPAN_CONTEXT_KEY, traceInfo3);
        spanTraceMap = LogUtils.parseTraceIdSpanId(request);
        assertNull(spanTraceMap.get(LogUtils.TCID));
        assertNull(spanTraceMap.get(LogUtils.SPID));

        String traceInfo4 = "spid=6d4eecae-5fcf-11eb-9e5c-3d7693ec528c";
        request.getAttachmentKv().put(LogUtils.PROBE_SPAN_CONTEXT_KEY, traceInfo4);
        spanTraceMap = LogUtils.parseTraceIdSpanId(request);
        assertNull(spanTraceMap.get(LogUtils.TCID));
        assertNull(spanTraceMap.get(LogUtils.SPID));

    }

    @Test
    public void serverMethodExceedTimeout() {
        Response response = new RpcResponse();
        response.getAttachmentKv().put(Constants.REQUEST_TIMEOUT_KEY, 5000);
        response.getAttachmentKv().put(Constants.EXECUTE_METHOD_COST, 6000L);
        response.getAttachmentKv().put(Constants.SERVER_EXEC_COST_KEY, 6010L);

        long beforeEncodeTime = System.currentTimeMillis();
        response.getAttachmentKv().put(Constants.BEFORE_ENCODE_HEADER_TIME_KEY, beforeEncodeTime);
        response.getAttachmentKv().put(Constants.SERVER_RECEIVE_REQ_TIME_KEY, beforeEncodeTime + 10);
        response.getAttachmentKv().put(Constants.TRACE_ID_KEY, "123456789");

        response.getNoneAdditionKv().put(Constants.RECEIVE_BYTE_MSG_TIME_KEY, beforeEncodeTime + 6010 + 10);
        response.getNoneAdditionKv().put(Constants.DECODE_HEADER_COST, 1L);
        response.getNoneAdditionKv().put(Constants.DECODE_BODY_COST, 1L);
        response.getNoneAdditionKv().put(Constants.REMOTE_ADDRESS_KEY, "localhost:9090");

        LogUtils.timeoutReqAdditionalLog(response);
    }

    @Test
    public void instanceStatusAbnormalTimeout() {
        Response response = new RpcResponse();
        response.getAttachmentKv().put(Constants.REQUEST_TIMEOUT_KEY, 5000);
        response.getAttachmentKv().put(Constants.EXECUTE_METHOD_COST, 600L);
        response.getAttachmentKv().put(Constants.SERVER_EXEC_COST_KEY, 6010L);

        long beforeEncodeTime = System.currentTimeMillis();
        response.getAttachmentKv().put(Constants.BEFORE_ENCODE_HEADER_TIME_KEY, beforeEncodeTime);
        response.getAttachmentKv().put(Constants.SERVER_RECEIVE_REQ_TIME_KEY, beforeEncodeTime + 10);
        response.getAttachmentKv().put(Constants.TRACE_ID_KEY, "123456789");

        response.getNoneAdditionKv().put(Constants.RECEIVE_BYTE_MSG_TIME_KEY, beforeEncodeTime + 6010 + 10);
        response.getNoneAdditionKv().put(Constants.DECODE_HEADER_COST, 1L);
        response.getNoneAdditionKv().put(Constants.DECODE_BODY_COST, 1L);
        response.getNoneAdditionKv().put(Constants.REMOTE_ADDRESS_KEY, "localhost:9090");

        LogUtils.timeoutReqAdditionalLog(response);
    }

    @Test
    public void messageDelayTimeout() {
        Response response = new RpcResponse();
        response.getAttachmentKv().put(Constants.REQUEST_TIMEOUT_KEY, 5000);
        response.getAttachmentKv().put(Constants.EXECUTE_METHOD_COST, 600L);
        response.getAttachmentKv().put(Constants.SERVER_EXEC_COST_KEY, 610L);

        long beforeEncodeTime = System.currentTimeMillis();
        response.getAttachmentKv().put(Constants.BEFORE_ENCODE_HEADER_TIME_KEY, beforeEncodeTime);
        response.getAttachmentKv().put(Constants.SERVER_RECEIVE_REQ_TIME_KEY, beforeEncodeTime + 3000);
        response.getAttachmentKv().put(Constants.TRACE_ID_KEY, "123456789");

        response.getNoneAdditionKv().put(Constants.RECEIVE_BYTE_MSG_TIME_KEY, beforeEncodeTime + 610 + 3000);
        response.getNoneAdditionKv().put(Constants.DECODE_HEADER_COST, 1L);
        response.getNoneAdditionKv().put(Constants.DECODE_BODY_COST, 1L);
        response.getNoneAdditionKv().put(Constants.REMOTE_ADDRESS_KEY, "localhost:9090");

        LogUtils.timeoutReqAdditionalLog(response);
    }

    @Test
    public void unknownTimeout() {
        Response response = new RpcResponse();
        response.getAttachmentKv().put(Constants.REQUEST_TIMEOUT_KEY, 5000);
        response.getAttachmentKv().put(Constants.EXECUTE_METHOD_COST, 600L);
        response.getAttachmentKv().put(Constants.SERVER_EXEC_COST_KEY, 610L);

        long beforeEncodeTime = System.currentTimeMillis();
        response.getAttachmentKv().put(Constants.BEFORE_ENCODE_HEADER_TIME_KEY, beforeEncodeTime);
        response.getAttachmentKv().put(Constants.SERVER_RECEIVE_REQ_TIME_KEY, beforeEncodeTime + 500);
        response.getAttachmentKv().put(Constants.TRACE_ID_KEY, "123456789");

        response.getNoneAdditionKv().put(Constants.RECEIVE_BYTE_MSG_TIME_KEY, beforeEncodeTime + 610 + 500);
        response.getNoneAdditionKv().put(Constants.DECODE_HEADER_COST, 1L);
        response.getNoneAdditionKv().put(Constants.DECODE_BODY_COST, 1L);
        response.getNoneAdditionKv().put(Constants.REMOTE_ADDRESS_KEY, "localhost:9090");

        LogUtils.timeoutReqAdditionalLog(response);
    }

    @Test
    public void additionalNull() {
        Response response = new RpcResponse();
        LogUtils.timeoutReqAdditionalLog(response); // unknow reason
    }

}