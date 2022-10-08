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
 
package com.baidu.cloud.starlight.api.protocol;

import com.baidu.cloud.starlight.api.common.Constants;
import com.baidu.cloud.starlight.api.exception.CodecException;
import com.baidu.cloud.starlight.api.model.MsgBase;
import com.baidu.cloud.starlight.api.model.Request;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.utils.LogUtils;
import com.baidu.cloud.thirdparty.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by liuruisen on 2019/12/4.
 */
public interface ProtocolEncoder {

    Logger LOGGER = LoggerFactory.getLogger(ProtocolEncoder.class);

    /**
     * Encoding MsgBase into binary data for the protocol. Body encoding will be handed over to method
     * {@link #encodeBody(MsgBase)} 1.Determine the data type: Request or Response 2.Encode the MsgBase to ByteBuf
     * 3.Throw CodecException
     *
     * @param input
     * @return
     * @throws CodecException
     */
    ByteBuf encode(MsgBase input) throws CodecException;

    /**
     * Encode params to binary body. Get parameters from MsgBase, encode them and save them. Construct and Encode
     *
     * @param msgBase
     * @throws CodecException
     */
    void encodeBody(MsgBase msgBase) throws CodecException;

    /**
     * Starlight用于满足各种功能，额外向response kv中添加的内容
     * 
     * @param response
     */
    default void addAdditionalRespKv(Response response) {
        Map<String, Object> respExtKv = response.getAttachmentKv();
        try {
            if (response.getRequest() != null) {
                Request request = response.getRequest();
                // server receive req time
                Long recvReqTime = request.getNoneAdditionKv().get(Constants.RECEIVE_BYTE_MSG_TIME_KEY) == null ? null
                    : ((Long) request.getNoneAdditionKv().get(Constants.RECEIVE_BYTE_MSG_TIME_KEY));

                // 近似值, 因为encode时获取不到最终发送请求的时间，采取encode header的时间
                Long retRespTime = response.getNoneAdditionKv().get(Constants.BEFORE_ENCODE_HEADER_TIME_KEY) == null
                    ? null : ((Long) response.getNoneAdditionKv().get(Constants.BEFORE_ENCODE_HEADER_TIME_KEY));

                Long serverExecCost = null;
                if (recvReqTime != null && retRespTime != null) { // 近似值
                    serverExecCost = retRespTime - recvReqTime;
                }

                Long executeMethodCost = request.getNoneAdditionKv().get(Constants.EXECUTE_METHOD_COST) == null ? null
                    : ((Long) request.getNoneAdditionKv().get(Constants.EXECUTE_METHOD_COST));

                Map<String, String> tidSpid = LogUtils.parseTraceIdSpanId(request);

                // 服务端回传给客户端的kv对不宜过多，框架层不能引入过多payload
                respExtKv.put(Constants.SERVER_RECEIVE_REQ_TIME_KEY, recvReqTime);
                respExtKv.put(Constants.SERVER_EXEC_COST_KEY, serverExecCost);
                respExtKv.put(Constants.EXECUTE_METHOD_COST, executeMethodCost);
                respExtKv.put(Constants.TRACE_ID_KEY, tidSpid.get(LogUtils.TCID));
                respExtKv.put(Constants.REQUEST_TIMEOUT_KEY,
                    request.getAttachmentKv().get(Constants.REQUEST_TIMEOUT_KEY));
                respExtKv.put(Constants.BEFORE_ENCODE_HEADER_TIME_KEY, // approx client request time
                    request.getAttachmentKv().get(Constants.BEFORE_ENCODE_HEADER_TIME_KEY));
            }
        } catch (Throwable e) {
            LOGGER.warn("generateStarlightRespMeta from response failed, cause by ", e);
        }
    }
}
