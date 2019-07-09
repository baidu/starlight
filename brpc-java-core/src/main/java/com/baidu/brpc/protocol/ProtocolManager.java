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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by huwenwei on 2017/9/23.
 */
@Slf4j
public class ProtocolManager {
    private Map<Integer, ProtocolFactory> protocolFactoryMap = new HashMap<Integer, ProtocolFactory>();
    private Map<Integer, Protocol> protocolMap = new HashMap<Integer, Protocol>();
    private List<Protocol> coexistenceProtocols = new ArrayList<Protocol>();
    private int coexistenceProtocolSize = 0;

    private static ProtocolManager instance;

    public static ProtocolManager getInstance() {
        if (instance == null) {
            synchronized (ProtocolManager.class) {
                if (instance == null) {
                    instance = new ProtocolManager();
                }
            }
        }
        return instance;
    }

    private ProtocolManager() {
    }

    /**
     * application can register custom protocol
     */
    public void registerProtocol(ProtocolFactory protocolFactory, String encoding) {
        Integer protocolType = protocolFactory.getProtocolType();
        if (protocolFactoryMap.get(protocolType) != null) {
            throw new RuntimeException("protocol exist, type=" + protocolType);
        }
        Protocol protocol = protocolFactory.createProtocol(encoding);
        protocolMap.put(protocolType, protocol);
        if (protocol.isCoexistence()) {
            coexistenceProtocols.add(protocol);
            coexistenceProtocolSize++;
        }
        log.info("register protocol:{} success", protocolType);
    }

    public Protocol getProtocol(Integer protocolType) {
        Protocol protocol = protocolMap.get(protocolType);
        if (protocol != null) {
            return protocol;
        }

        throw new RuntimeException("protocol not exist, type=" + protocolType);
    }

    public Map<Integer, Protocol> getProtocolMap() {
        return protocolMap;
    }

    public List<Protocol> getCoexistenceProtocols() {
        return coexistenceProtocols;
    }

    public int getCoexistenceProtocolSize() {
        return coexistenceProtocolSize;
    }

}
