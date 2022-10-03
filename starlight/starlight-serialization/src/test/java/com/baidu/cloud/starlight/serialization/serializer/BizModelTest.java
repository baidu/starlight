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
 
package com.baidu.cloud.starlight.serialization.serializer;

import com.baidu.cloud.starlight.serialization.serializer.model.BasePageRequestDto;
import com.baidu.cloud.starlight.serialization.serializer.model.OrderBy;
import com.baidu.cloud.thirdparty.google.common.collect.Lists;
import org.junit.Test;

import java.util.Arrays;

/**
 * Created by liuruisen on 2021/2/4.
 */
public class BizModelTest extends NewProtostuffSerializeTest {

    @Test
    public void basePageRequestDto() {
        BasePageRequestDto basePageRequestDto = new BasePageRequestDto(26L, "TOKEN");
        basePageRequestDto.setPageNo(1);
        basePageRequestDto.setPageSize(10000);
        basePageRequestDto.setOrderBys(Lists.newArrayList(new OrderBy("id", OrderBy.OrderType.ASC)));

        byte[] nullBytes = serialize(basePageRequestDto, BasePageRequestDto.class, nullStrategy);
        System.out.println(Arrays.toString(nullBytes));

        BasePageRequestDto requestDto =
            (BasePageRequestDto) deserialize(nullBytes, BasePageRequestDto.class, nullStrategy);
        System.out.println(requestDto);
    }
}
