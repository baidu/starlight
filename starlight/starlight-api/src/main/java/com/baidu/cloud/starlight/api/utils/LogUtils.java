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
import com.baidu.cloud.starlight.api.model.MsgBase;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by liuruisen on 2021/1/7.
 */
public class LogUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogUtils.class);

    // 凤睛插件的spanContext key
    protected static final String PROBE_SPAN_CONTEXT_KEY = "traceSpanContext";

    protected static final String X_B3_TRACE_ID = "X-B3-TraceId";

    protected static final String X_B3_SPAN_ID = "X-B3-SpanId";

    private static final String EQUAL_CHARACTER = "=";

    private static final String AND_CHARACTER = "&";

    public static final String TCID = "tcid";

    public static final String SPID = "spid";

    public static void addLogTimeAttachment(MsgBase msg, String timeKey, long timestamp) {
        if (msg.getNoneAdditionKv() == null) {
            msg.setNoneAdditionKv(new HashMap<>());
        }

        msg.getNoneAdditionKv().put(timeKey, timestamp);
    }

    public static void recordAccessLog(Request request, Response response) {
        String accessLogFormat = "[ACCLOG] recvTime {}, respTime {}, remoteName {}, remoteAddr {}, " + "protocol {}, "
            + "status {}, cost {}, " + "req {}:{}, " + "decReqHead {}, " + "waitForExec {}, " + "decodeReqBody {}, "
            + "execServFilter {}, " + "execMethod {}, " + "encRespBody {}, " + "waitForIoExec {}, " + "encRespHead {}, "
            + "tid {}, spid {}";

        try {
            Map<String, String> traceSpanIdMap = parseTraceIdSpanId(request);

            Long decodeRequestHeaderCost = request.getNoneAdditionKv().get(Constants.DECODE_HEADER_COST) == null ? null
                : ((Long) request.getNoneAdditionKv().get(Constants.DECODE_HEADER_COST));

            Long decodeRequestBodyCost = request.getNoneAdditionKv().get(Constants.DECODE_BODY_COST) == null ? null
                : ((Long) request.getNoneAdditionKv().get(Constants.DECODE_BODY_COST));

            Long waitForExecTime = request.getNoneAdditionKv().get(Constants.WAIT_FOR_THREAD_COST) == null ? null
                : ((Long) request.getNoneAdditionKv().get(Constants.WAIT_FOR_THREAD_COST));

            Long executeMethodCost = request.getNoneAdditionKv().get(Constants.EXECUTE_METHOD_COST) == null ? null
                : ((Long) request.getNoneAdditionKv().get(Constants.EXECUTE_METHOD_COST));

            Long encodeResponseBodyCost = response.getNoneAdditionKv().get(Constants.ENCODE_BODY_COST) == null ? null
                : ((Long) response.getNoneAdditionKv().get(Constants.ENCODE_BODY_COST));

            Long encodeResponseHeaderCost = response.getNoneAdditionKv().get(Constants.ENCODE_HEADER_COST) == null
                ? null : ((Long) response.getNoneAdditionKv().get(Constants.ENCODE_HEADER_COST));

            Long recvReqTime = request.getNoneAdditionKv().get(Constants.RECEIVE_BYTE_MSG_TIME_KEY) == null ? null
                : ((Long) request.getNoneAdditionKv().get(Constants.RECEIVE_BYTE_MSG_TIME_KEY));

            Long retRespTime = response.getNoneAdditionKv().get(Constants.RETURN_RESPONSE_TIME_KEY) == null ? null
                : ((Long) response.getNoneAdditionKv().get(Constants.RETURN_RESPONSE_TIME_KEY));

            Long serverExecCost = null;
            if (recvReqTime != null && retRespTime != null) {
                serverExecCost = retRespTime - recvReqTime;
            }

            LOGGER.info(accessLogFormat, recvReqTime == null ? null : timestampToString(recvReqTime), // recvReqTime
                retRespTime == null ? null : timestampToString(retRespTime), // retRespTime
                request.getAttachmentKv().remove(Constants.CONSUMER_APP_NAME_KEY), // consumer name
                request.getNoneAdditionKv().get(Constants.REMOTE_ADDRESS_KEY), // consumer addr
                request.getProtocolName(), // protocol
                response.getStatus(), // status
                serverExecCost, // server exec all cost
                simpleServiceName(request.getServiceName()), // req service name
                request.getMethodName(), // req method name
                decodeRequestHeaderCost, // decode req header cost
                waitForExecTime, // wait for exec
                decodeRequestBodyCost, // decode req body
                request.getNoneAdditionKv().get(Constants.SERVER_FILTER_EXEC_COST_KEY), // server filter exec cost
                executeMethodCost, // exec method cost
                encodeResponseBodyCost, // encode response cost
                response.getNoneAdditionKv().get(Constants.WAIT_FOR_IO_THREAD_COST_KEY), // wait for io exec cost
                encodeResponseHeaderCost, // encode resp header cost
                traceSpanIdMap.get(TCID), traceSpanIdMap.get(SPID));

            LOGGER.debug("[ACCLOG] Request logKv {}, Response logKv {}", request.getNoneAdditionKv(),
                response.getNoneAdditionKv());
        } catch (Exception e) {
            LOGGER.warn("Record server access log error, cause by {}", e.getMessage());
        }
    }

    public static void recordRequestLog(Request request, Response response) {

        String requestLogFormat = "[REQLOG] reqTime {}, recvTime {}, remoteName {}, remoteAddr {}, " + "protocol {}, "
            + "status {}, cost {}, " + "req {}:{}, " + "encReqBody {}, " + "waitForIoExec {}, " + "encReqHead {}, "
            + "serverExec {}, " + "decRespHead {}, " + "waitForExec {}, " + "decRespBody {}, " + "tid {}, spid {}";
        try {
            Map<String, String> traceSpanIdMap = parseTraceIdSpanId(request);

            Long encodeRequestBodyCost = request.getNoneAdditionKv().get(Constants.ENCODE_BODY_COST) == null ? null
                : ((Long) request.getNoneAdditionKv().get(Constants.ENCODE_BODY_COST));
            Long encodeRequestHeaderCost = request.getNoneAdditionKv().get(Constants.ENCODE_HEADER_COST) == null ? null
                : ((Long) request.getNoneAdditionKv().get(Constants.ENCODE_HEADER_COST));
            Long waitForThreadCost = response.getNoneAdditionKv().get(Constants.WAIT_FOR_THREAD_COST) == null ? null
                : ((Long) response.getNoneAdditionKv().get(Constants.WAIT_FOR_THREAD_COST));
            Long decodeResponseHeaderCost = response.getNoneAdditionKv().get(Constants.DECODE_HEADER_COST) == null
                ? null : ((Long) response.getNoneAdditionKv().get(Constants.DECODE_HEADER_COST));
            Long decodeResponseBodyCost = response.getNoneAdditionKv().get(Constants.DECODE_BODY_COST) == null ? null
                : ((Long) response.getNoneAdditionKv().get(Constants.DECODE_BODY_COST));

            Long recvRespTime = response.getNoneAdditionKv().get(Constants.RECEIVE_BYTE_MSG_TIME_KEY) == null ? null
                : ((Long) response.getNoneAdditionKv().get(Constants.RECEIVE_BYTE_MSG_TIME_KEY));

            Long beforeCallServerTime =
                request.getNoneAdditionKv().get(Constants.BEFORE_SERVER_EXECUTE_TIME_KEY) == null ? null
                    : ((Long) request.getNoneAdditionKv().get(Constants.BEFORE_SERVER_EXECUTE_TIME_KEY));

            Long serverExecCost = null;
            if (beforeCallServerTime != null && recvRespTime != null) {
                serverExecCost = recvRespTime - beforeCallServerTime;
            }

            LOGGER.info(requestLogFormat, // reqlog
                beforeCallServerTime == null ? null : timestampToString(beforeCallServerTime), // reqtime
                recvRespTime == null ? null : timestampToString(recvRespTime), // respTime
                request.getAttachmentKv().remove(Constants.PROVIDER_APP_NAME_KEY), // remoteName
                request.getNoneAdditionKv().get(Constants.REMOTE_ADDRESS_KEY), // remoteAddr
                request.getProtocolName(), // protocol
                response.getStatus(), // status
                request.getNoneAdditionKv().get(Constants.CLIENT_REQUEST_COST), // totalCost
                simpleServiceName(request.getServiceName()), // reqServiceName
                request.getMethodName(), // reqMethodName
                encodeRequestBodyCost, // encodeReqBody
                request.getNoneAdditionKv().get(Constants.WAIT_FOR_IO_THREAD_COST_KEY), // waitForIoExec
                encodeRequestHeaderCost, // encodeReqHeader
                serverExecCost, // serverExecCost
                decodeResponseHeaderCost, // decodeRespHeader
                waitForThreadCost, // waitForClientThread
                decodeResponseBodyCost, // decodeRespBody
                traceSpanIdMap.get(TCID), // tid
                traceSpanIdMap.get(SPID)); // spid

            LOGGER.debug("[REQLOG] Request logKv {}, Response logKv {}", request.getNoneAdditionKv(),
                response.getNoneAdditionKv());
        } catch (Exception e) {
            LOGGER.warn("Record server access log error, cause by {}", e.getMessage());
        }
    }

    /**
     * 请求已经超时后收到了响应，打印响应信息，并推断超时原因
     *
     * @param response
     */
    public static void timeoutReqAdditionalLog(Response response) {
        try {
            String timeoutLog = "[TIMEOUT] Request {} timeout: {}. " + "clientReqTime(approx) {}, "
                + "servRecvTime {}, " + "servCost(approx) {}, " + "servMethodExecCost {}, " + "recvTime {}, "
                + "decRespHead {}, " + "decRespBody {}, " + "tid {}";

            String timeoutCause = null;
            Object methodExecCostObj = response.getAttachmentKv().get(Constants.EXECUTE_METHOD_COST);
            Object reqTimeoutObj = response.getAttachmentKv().get(Constants.REQUEST_TIMEOUT_KEY);
            Object servCostObj = response.getAttachmentKv().get(Constants.SERVER_EXEC_COST_KEY);

            Integer requestTimeout = null;
            Long methodExecCost = null;
            Long serverCost = null;

            if (reqTimeoutObj instanceof Integer && methodExecCostObj instanceof Long) { // 方法执行时间超过超时时间，业务问题
                requestTimeout = (Integer) reqTimeoutObj;
                methodExecCost = (Long) methodExecCostObj;

                if (methodExecCost >= requestTimeout) {
                    timeoutCause = "4801 Biz method execution timeout(" + methodExecCost + ">" + requestTimeout + "ms)";
                }
            }

            if (methodExecCostObj instanceof Long && servCostObj instanceof Long && StringUtils.isEmpty(timeoutCause)) {
                serverCost = (Long) servCostObj;
                methodExecCost = (Long) methodExecCostObj;

                if ((serverCost - methodExecCost) > 1000) { // 差值大于1s，推测实例状态异常导致
                    timeoutCause = "4802 Abnormal server instance status(CPU_WAIT_IO, tasks_used or cpu_used)";
                }
            }

            Object clientReqTimeObj = response.getAttachmentKv().get(Constants.BEFORE_ENCODE_HEADER_TIME_KEY);
            Object servRecvReqTimeObj = response.getAttachmentKv().get(Constants.SERVER_RECEIVE_REQ_TIME_KEY);
            Long clientReqTime = null;
            Long servRecvReqTime = null;
            if (clientReqTimeObj instanceof Long && servRecvReqTimeObj instanceof Long // 不满足上述两个条件，推测收到消息延迟
                && StringUtils.isEmpty(timeoutCause)) {

                clientReqTime = (Long) clientReqTimeObj;
                servRecvReqTime = (Long) servRecvReqTimeObj;
                long messageDelay = servRecvReqTime - clientReqTime;
                if (messageDelay > 1000) {
                    timeoutCause = "4803 Rpc message delay(" + messageDelay + "ms)";
                }
            }

            if (StringUtils.isEmpty(timeoutCause)) {
                timeoutCause = "4810 Unknown reason";
            }

            Object recvRespTime = response.getNoneAdditionKv().get(Constants.RECEIVE_BYTE_MSG_TIME_KEY);
            Object traceId = response.getAttachmentKv().get(Constants.TRACE_ID_KEY);

            LOGGER.error(timeoutLog, response.getNoneAdditionKv().get(Constants.REMOTE_ADDRESS_KEY), // remote addr
                timeoutCause, // timeout reason
                clientReqTimeObj instanceof Long ? timestampToString((Long) clientReqTimeObj) : null, // req time
                // server recv time
                servRecvReqTimeObj instanceof Long ? timestampToString((Long) servRecvReqTimeObj) : null, servCostObj,
                methodExecCostObj,
                recvRespTime instanceof Long ? LogUtils.timestampToString((Long) recvRespTime) : recvRespTime,
                response.getNoneAdditionKv().get(Constants.DECODE_HEADER_COST),
                response.getNoneAdditionKv().get(Constants.DECODE_BODY_COST), traceId);
        } catch (Throwable e) {
            LOGGER.warn("Record server timeout log error, cause by ", e);
        }
    }

    public static String timestampToString(long timestamp) {
        Date date = new Date(timestamp);
        DateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");

        return sdf.format(date);
    }

    protected static String simpleServiceName(String originServiceName) {
        if (!originServiceName.contains(".")) {
            return originServiceName;
        }

        String[] splitNames = originServiceName.split("\\.");
        if (splitNames.length == 1) {
            return originServiceName;
        }

        return splitNames[splitNames.length - 1];
    }

    public static Map<String, String> parseTraceIdSpanId(Request request) {
        Map<String, String> traceIdSpanIdMap = new HashMap<>();
        if (request.getAttachmentKv() != null
            && request.getAttachmentKv().get(PROBE_SPAN_CONTEXT_KEY) instanceof String) {

            traceIdSpanIdMap = probeSpanContextToMap((String) request.getAttachmentKv().get(PROBE_SPAN_CONTEXT_KEY));
        }

        return traceIdSpanIdMap;
    }

    protected static Map<String, String> probeSpanContextToMap(String traceSpanContext) {

        Map<String, String> traceSpanMap = new HashMap<>();

        if (StringUtils.isBlank(traceSpanContext)) {
            return traceSpanMap;
        }

        if (traceSpanContext.endsWith(AND_CHARACTER)) {
            traceSpanContext = traceSpanContext.substring(0, traceSpanContext.length() - 1);
        }

        // traceSpanContext -> tcid=6d4eecaf-5fcf-11eb-9e5c-3d7693ec528c&spid=6d4eecae-5fcf-11eb-9e5c-3d7693ec528c&xx

        if (!traceSpanContext.contains("tcid") || !traceSpanContext.contains("spid")) {
            return traceSpanMap;
        }

        String[] traceSpans = traceSpanContext.split(AND_CHARACTER);
        if (traceSpans.length < 2) {
            return traceSpanMap;
        }

        String[] traceInfos = traceSpans[0].split(EQUAL_CHARACTER);
        if (traceInfos.length == 2) {
            traceSpanMap.put(traceInfos[0], traceInfos[1]);
        }
        String[] spanInfos = traceSpans[1].split(EQUAL_CHARACTER);
        if (spanInfos.length == 2) {
            traceSpanMap.put(spanInfos[0], spanInfos[1]);
        }

        return traceSpanMap;
    }

}
