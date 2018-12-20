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

package com.baidu.brpc.naming;

import com.baidu.brpc.client.EndPoint;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;

/**
 * Fetch service list from List Naming Service
 */
public class ListNamingService implements NamingService {
    private List<EndPoint> endPoints;

    public ListNamingService(BrpcURI namingUrl) {
        Validate.notNull(namingUrl);
        Validate.notEmpty(namingUrl.getHosts());
        Validate.notEmpty(namingUrl.getPorts());
        int size = namingUrl.getHosts().size();
        Validate.isTrue(size == namingUrl.getPorts().size());
        this.endPoints = new ArrayList<EndPoint>(size);
        for (int i = 0; i < size; i++) {
            String host = namingUrl.getHosts().get(i);
            String portString = namingUrl.getPorts().get(i);
            int port;
            if (StringUtils.isNotBlank(portString)) {
                port = Integer.valueOf(portString);
            } else {
                port = 80;
            }
            endPoints.add(new EndPoint(host, port));
        }
    }

    @Override
    public List<EndPoint> lookup(RegisterInfo registerInfo) {
        return endPoints;
    }

    @Override
    public void subscribe(RegisterInfo registerInfo, final NotifyListener listener) {
    }

    @Override
    public void unsubscribe(RegisterInfo registerInfo) {
    }

    @Override
    public void register(RegisterInfo registerInfo) {
    }

    @Override
    public void unregister(RegisterInfo registerInfo) {
    }
}
