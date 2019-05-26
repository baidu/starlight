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
package com.baidu.brpc.example.stargate;

import com.baidu.brpc.RpcContext;
import com.baidu.brpc.utils.GsonUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class StargateDemoServiceImpl implements StargateDemoService {

    @Override
    public StargateDemoResDto call(StargateDemoReqDto reqDto) {
        if (log.isDebugEnabled()) {
            log.debug("rev from server {}", GsonUtils.toJson(reqDto));
            RpcContext rpcContext = RpcContext.getContext();
            Map<String, Object> attachment = rpcContext.getRequestKvAttachment();
            if (attachment != null) {
                log.info("request attachment:{}", attachment.get("key"));
                rpcContext.setResponseKvAttachment("resKey", attachment.get("key"));
            }
        }
        return StargateDemoResDto.builder().id(reqDto.getId()).name(reqDto.getName()).build();
    }

    @Override
    public List<StargateDemoResDto> list(StargateDemoReqDto reqDto) {
        if (log.isDebugEnabled()) {
            log.info("rev from server {}", GsonUtils.toJson(reqDto));
        }
        List<StargateDemoResDto> list = new ArrayList<StargateDemoResDto>();
        for (int i = 0; i < reqDto.getId(); i++) {
            list.add(StargateDemoResDto.builder().id(reqDto.getId()).name(reqDto.getName()).build());
        }
        return list;
    }

    @Override
    public Map<Long, StargateDemoResDto> map(StargateDemoReqDto reqDto) {
        if (log.isDebugEnabled()) {
            log.info("rev from server {}", GsonUtils.toJson(reqDto));
        }
        Map<Long, StargateDemoResDto> map = new HashMap<Long, StargateDemoResDto>();
        StargateDemoResDto build = StargateDemoResDto.builder().id(reqDto.getId()).name(reqDto.getName()).build();
        map.put(reqDto.getId(), build);
        return map;
    }
}
