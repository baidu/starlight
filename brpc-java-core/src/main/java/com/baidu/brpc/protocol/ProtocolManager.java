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

package com.baidu.brpc.protocol;

import com.baidu.brpc.protocol.http.HttpRpcProtocol;
import com.baidu.brpc.protocol.hulu.HuluRpcProtocol;
import com.baidu.brpc.protocol.nshead.NSHeadRpcProtocol;
import com.baidu.brpc.protocol.sofa.SofaRpcProtocol;
import com.baidu.brpc.protocol.standard.BaiduRpcProtocol;
import com.baidu.brpc.protocol.stargate.StargateRpcProtocol;
import lombok.Getter;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Created by huwenwei on 2017/9/23.
 */
public class ProtocolManager {
    private Map<Integer, Protocol> protocolMap;
    private List<Protocol> protocols;
    private int protocolNum;

    private static ProtocolManager instance;
    private boolean isInit;

    /**
     * no need to be thread safe, since it is called when bootstrap
     */
    public static ProtocolManager instance() {
        if (instance == null) {
            instance = new ProtocolManager();
        }
        return instance;
    }

    private ProtocolManager() {

    }

    public ProtocolManager init(String encoding) {
        if (!isInit) {
            protocolMap = new HashMap<Integer, Protocol>(64);
            protocols = new ArrayList<Protocol>(64);
            protocolMap.put(Options.ProtocolType.PROTOCOL_BAIDU_STD_VALUE, new BaiduRpcProtocol());
            protocolMap.put(Options.ProtocolType.PROTOCOL_SOFA_PBRPC_VALUE, new SofaRpcProtocol());
            protocolMap.put(Options.ProtocolType.PROTOCOL_HULU_PBRPC_VALUE, new HuluRpcProtocol());
            protocolMap.put(Options.ProtocolType.PROTOCOL_HTTP_JSON_VALUE,
                    new HttpRpcProtocol(Options.ProtocolType.PROTOCOL_HTTP_JSON_VALUE, encoding));
            protocolMap.put(Options.ProtocolType.PROTOCOL_HTTP_PROTOBUF_VALUE,
                    new HttpRpcProtocol(Options.ProtocolType.PROTOCOL_HTTP_PROTOBUF_VALUE, encoding));
            protocolMap.put(Options.ProtocolType.PROTOCOL_NSHEAD_PROTOBUF_VALUE,
                    new NSHeadRpcProtocol(Options.ProtocolType.PROTOCOL_NSHEAD_PROTOBUF_VALUE, encoding));
            protocolMap.put(Options.ProtocolType.PROTOCOL_NSHEAD_JSON_VALUE,
                    new NSHeadRpcProtocol(Options.ProtocolType.PROTOCOL_NSHEAD_JSON_VALUE, encoding));
            protocols.addAll(protocolMap.values());
            protocolNum = protocols.size();
            isInit = true;
        }
        return this;
    }

    // application can register custom protocol
    public void registerProtocol(Integer protocolType, Protocol protocol) {
        if (protocolMap.get(protocolType) != null) {
            throw new RuntimeException("protocol exist, type=" + protocolType);
        }
        protocolMap.put(protocolType, protocol);
        protocols.add(protocol);
        protocolNum++;
    }

    public Protocol getProtocol(Integer protocolType) {
        Protocol protocol = protocolMap.get(protocolType);
        if (protocol != null) {
            return protocol;
        }

        // 不共存协议判断
        // 如果有例如Stargate的协议，可能存在冲突情况，在指明协议的情况，使用类加载机制生产协议单例
        if (Options.ProtocolType.PROTOCOL_STARGATE_VALUE == protocolType) {
            return UncoexistenceStargate.INSTANCE.getProtocol();
        }

        throw new RuntimeException("protocol not exist, type=" + protocolType);
    }

    public Map<Integer, Protocol> getProtocolMap() {
        return protocolMap;
    }

    public List<Protocol> getProtocols() {
        return protocols;
    }

    public int getProtocolNum() {
        return protocolNum;
    }

    @Getter
    enum UncoexistenceStargate {
        INSTANCE;

        private Protocol protocol;

        UncoexistenceStargate() {
            protocol = new StargateRpcProtocol();
        }
    }
}
