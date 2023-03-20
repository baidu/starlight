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
 
package com.baidu.cloud.starlight.protocol.brpc;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.exception.StarlightRpcException;
import com.baidu.cloud.starlight.api.exception.TransportException;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Status code mapping between Starlight and Brpc Created by liuruisen on 2020/11/9.
 */
public class CodeMapping {

    private static final Logger LOGGER = LoggerFactory.getLogger(CodeMapping.class);

    // Errno caused by client
    /*
        Service not found
    */
    public static final Integer ENOSERVICE = 1001;
    /*
        Method not found
     */
    public static final Integer ENOMETHOD = 1002;
    /*
        Bad Request
     */
    public static final Integer EREQUEST = 1003;
    /*
        Unauthorized
     */
    public static final Integer EAUTH = 1004;
    /*
        Too many sub calls failed
     */
    public static final Integer ETOOMANYFAILS = 1005;
    /*
        [Internal] ParallelChannel finished
     */
    public static final Integer EPCHANFINISH = 1006;
    /*
        Sending backup request
     */
    public static final Integer EBACKUPREQUEST = 1007;
    /*
        RPC call is timed out
     */
    public static final Integer ERPCTIMEDOUT = 1008;
    /*
        Broken socket
     */
    public static final Integer EFAILEDSOCKET = 1009;
    /*
        Bad http call
     */
    public static final Integer EHTTP = 1010;
    /*
        The server is overcrowded
     */
    public static final Integer EOVERCROWDED = 1011;
    /*
        RtmpRetryingClientStream is publishable
     */
    public static final Integer ERTMPPUBLISHABLE = 1012;
    /*
        createStream was rejected by the RTMP server
     */
    public static final Integer ERTMPCREATESTREAM = 1013;
    /*
        Got EOF
     */
    public static final Integer EEOF = 1014;
    /*
        The socket was not needed
     */
    public static final Integer EUNUSED = 1015;
    /*
        SSL related error
     */
    public static final Integer ESSL = 1016;

    // Errno caused by server
    /*
        Internal Server Error
     */
    public static final Integer EINTERNAL = 2001;
    /*
        Bad Response
     */
    public static final Integer ERESPONSE = 2002;
    /*
        Server is stopping
     */
    public static final Integer ELOGOFF = 2003;
    /*
        Reached server's limit on resources
     */
    public static final Integer ELIMIT = 2004;
    /*
        Close socket initiatively
     */
    public static final Integer ECLOSE = 2005;
    /*
        Failed Itp response
     */
    public static final Integer EITP = 2006;

    private static final BiMap<Integer, Integer> starlightToBrpcCodeMap;

    static {
        starlightToBrpcCodeMap = HashBiMap.create();
        starlightToBrpcCodeMap.put(StarlightRpcException.BAD_REQUEST, EREQUEST);
        starlightToBrpcCodeMap.put(StarlightRpcException.SERVICE_NOT_FOUND_EXCEPTION, ENOSERVICE);
        starlightToBrpcCodeMap.put(StarlightRpcException.METHOD_NOT_FOUND_EXCEPTION, ENOMETHOD);
        starlightToBrpcCodeMap.put(StarlightRpcException.INTERNAL_SERVER_ERROR, EINTERNAL);
        starlightToBrpcCodeMap.put(StarlightRpcException.TIME_OUT_EXCEPTION, ERPCTIMEDOUT);
    }

    /**
     * Get the mapped value of StarlightNo
     * 
     * @param starlightNo
     * @return BrpcNo
     */
    public static Integer getBrpcMappingOfStarlightNo(Integer starlightNo) {

        if (starlightNo.equals(Constants.SUCCESS_CODE)) {
            return 0;
        }

        Integer mappingNo = starlightToBrpcCodeMap.get(starlightNo);
        if (mappingNo != null) {
            return mappingNo;
        }

        if (starlightNo >= TransportException.CONNECT_EXCEPTION) {
            return EFAILEDSOCKET;
        }

        return EINTERNAL;
    }

    /**
     * Get the mapped value of BrpcNo
     * 
     * @param brpcNo
     * @return StarlightNo
     */
    public static Integer getStarlightMappingOfBrpcNo(Integer brpcNo) {

        if (brpcNo.equals(0)) {
            return Constants.SUCCESS_CODE;
        }

        if (brpcNo.equals(Constants.SUCCESS_CODE)) {
            LOGGER.warn("The brpc return code is 200, please make sure this is correct. "
                + "Maybe use an old version in sever(less than 2020.0.1-SNAPSHOT)");
            return Constants.SUCCESS_CODE;
        }

        Integer mappingNo = starlightToBrpcCodeMap.inverse().get(brpcNo);
        if (mappingNo != null) {
            return mappingNo;
        }

        return StarlightRpcException.INTERNAL_SERVER_ERROR;
    }
}
