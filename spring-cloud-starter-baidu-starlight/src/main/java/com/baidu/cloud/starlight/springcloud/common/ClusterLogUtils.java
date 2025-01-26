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
 
package com.baidu.cloud.starlight.springcloud.common;

import com.baidu.cloud.starlight.api.transport.PeerStatus;
import com.baidu.cloud.starlight.springcloud.client.outlier.OutlierDetectEvent;
import org.slf4j.Logger;
import org.springframework.cloud.client.ServiceInstance;

/**
 * Created by liuruisen on 2021/9/7.
 */
public class ClusterLogUtils {

    public static void logOutlierInstanceEject(Logger logger, String clientName,
                                               ServiceInstance serverInstance, PeerStatus status) {
        String outlierReason = "unknow";
        if (status.getStatusReason() instanceof OutlierDetectEvent) {
            outlierReason = ((OutlierDetectEvent) status.getStatusReason()).outlierReason();
        }
        logger.info("[OUTLIER] Outlier instance eject: "
                        + "remoteName {}, instance {}, outlierTime {}, reason {}",
                clientName, serverInstance.getHost() + ":" + serverInstance.getPort(),
                status.getStatusRecordTime(),
                outlierReason);
    }

    public static void logOutlierAppEject(Logger logger, String clientName,
                                          Integer originSize, Integer ejectCount, Integer maxEjectCount) {
        logger.info("[OUTLIER] Outlier app eject: remoteName {}, instanceNum {}, "
                        + "ejectNum {}, maxEjectNum {}",
                clientName, originSize, ejectCount, maxEjectCount);
    }

    public static void logOutlierRecoverySucc(Logger logger, String clientName,
                                              ServiceInstance serviceInstance,
                                              String recoveryReason,
                                              Integer lastEjectTime) {
        logger.info("[OUTLIER] Outlier recover succ: remoteName {}, instance {}, reason {}, lastEjectTime {}s",
                clientName, serviceInstance.getHost() + ":" + serviceInstance.getPort(),
                recoveryReason, lastEjectTime);
    }

    public static void logOutlierRecoveryFail(Logger logger, String clientName,
                                              ServiceInstance serverInstance, String failReason,
                                              Integer lastEjectTime, Integer nextEjectTime) {
        logger.info("[OUTLIER] Outlier recover fail: remoteName {}, instance {}, reason {}, "
                        + "lastEjectTime {}s, nextEjectTime {}s",
                clientName, serverInstance.getHost() + ":" + serverInstance.getPort(),
                failReason, lastEjectTime, nextEjectTime);
    }

}
