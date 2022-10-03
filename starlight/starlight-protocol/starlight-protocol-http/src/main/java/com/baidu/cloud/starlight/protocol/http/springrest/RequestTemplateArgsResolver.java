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
 
package com.baidu.cloud.starlight.protocol.http.springrest;

import com.baidu.cloud.thirdparty.feign.MethodMetadata;
import com.baidu.cloud.thirdparty.feign.Param;
import com.baidu.cloud.thirdparty.feign.QueryMapEncoder;
import com.baidu.cloud.thirdparty.feign.RequestTemplate;
import com.baidu.cloud.thirdparty.feign.Target;
import com.baidu.cloud.thirdparty.feign.Util;
import com.baidu.cloud.thirdparty.feign.codec.EncodeException;
import com.baidu.cloud.thirdparty.feign.template.UriUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Build {@link RequestTemplate} by resolving args. Use method parameters to improve {@link RequestTemplate}. Migrant
 * from {@link com.baidu.cloud.thirdparty.feign.ReflectiveFeign}, because it's native scope is protected we can't call
 * directly.
 */
public class RequestTemplateArgsResolver {

    // A QueryMapEncoder encodes Objects into maps of query parameter names to values.
    private final QueryMapEncoder queryMapEncoder;

    // target service interface
    protected final Target<?> target;

    public RequestTemplateArgsResolver(QueryMapEncoder queryMapEncoder, Target<?> target) {
        this.target = target;
        this.queryMapEncoder = queryMapEncoder;

    }

    public RequestTemplate create(Object[] argv, MethodMetadata metadata) {
        RequestTemplate mutable = RequestTemplate.from(metadata.template());
        mutable.feignTarget(target);
        if (metadata.urlIndex() != null) {
            int urlIndex = metadata.urlIndex();
            Util.checkArgument(argv[urlIndex] != null, "URI parameter %s was null", urlIndex);
            mutable.target(String.valueOf(argv[urlIndex]));
        }

        Map<Integer, Param.Expander> indexToExpander = indexToExpander(metadata);
        Map<String, Object> varBuilder = new LinkedHashMap<String, Object>();
        for (Map.Entry<Integer, Collection<String>> entry : metadata.indexToName().entrySet()) {
            int i = entry.getKey();
            Object value = argv[entry.getKey()];
            if (value != null) { // Null values are skipped.
                if (indexToExpander.containsKey(i)) {
                    value = expandElements(indexToExpander.get(i), value);
                }
                for (String name : entry.getValue()) {
                    varBuilder.put(name, value);
                }
            }
        }
        // Resolve url: Resolve all expressions using the variable value substitutions provided.
        RequestTemplate template = resolve(argv, mutable, varBuilder, metadata);
        if (metadata.queryMapIndex() != null) {
            // add query map parameters after initial resolve so that they take
            // precedence over any predefined values
            Object value = argv[metadata.queryMapIndex()];
            Map<String, Object> queryMap = toQueryMap(value);
            template = addQueryMapQueryParameters(queryMap, template, metadata);
        }

        if (metadata.headerMapIndex() != null) {
            template = addHeaderMapHeaders((Map<String, Object>) argv[metadata.headerMapIndex()], template);
        }

        return template;
    }

    private Map<Integer, Param.Expander> indexToExpander(MethodMetadata metadata) {
        Map<Integer, Param.Expander> indexToExpander = new LinkedHashMap<Integer, Param.Expander>();
        if (metadata.indexToExpander() != null) {
            indexToExpander.putAll(metadata.indexToExpander());
            return indexToExpander;
        }
        if (metadata.indexToExpanderClass().isEmpty()) {
            return indexToExpander;
        }
        for (Map.Entry<Integer, Class<? extends Param.Expander>> indexToExpanderClass : metadata.indexToExpanderClass()
            .entrySet()) {
            try {
                indexToExpander.put(indexToExpanderClass.getKey(), indexToExpanderClass.getValue().newInstance());
            } catch (InstantiationException e) {
                throw new IllegalStateException(e);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        return indexToExpander;
    }

    private Map<String, Object> toQueryMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        try {
            return queryMapEncoder.encode(value);
        } catch (EncodeException e) {
            throw new IllegalStateException(e);
        }
    }

    private Object expandElements(Param.Expander expander, Object value) {
        if (value instanceof Iterable) {
            return expandIterable(expander, (Iterable) value);
        }
        return expander.expand(value);
    }

    private List<String> expandIterable(Param.Expander expander, Iterable value) {
        List<String> values = new ArrayList<String>();
        for (Object element : value) {
            if (element != null) {
                values.add(expander.expand(element));
            }
        }
        return values;
    }

    @SuppressWarnings("unchecked")
    private RequestTemplate addHeaderMapHeaders(Map<String, Object> headerMap, RequestTemplate mutable) {
        for (Map.Entry<String, Object> currEntry : headerMap.entrySet()) {
            Collection<String> values = new ArrayList<String>();

            Object currValue = currEntry.getValue();
            if (currValue instanceof Iterable<?>) {
                Iterator<?> iter = ((Iterable<?>) currValue).iterator();
                while (iter.hasNext()) {
                    Object nextObject = iter.next();
                    values.add(nextObject == null ? null : nextObject.toString());
                }
            } else {
                values.add(currValue == null ? null : currValue.toString());
            }

            mutable.header(currEntry.getKey(), values);
        }
        return mutable;
    }

    @SuppressWarnings("unchecked")
    private RequestTemplate addQueryMapQueryParameters(Map<String, Object> queryMap, RequestTemplate mutable,
        MethodMetadata metadata) {
        for (Map.Entry<String, Object> currEntry : queryMap.entrySet()) {
            Collection<String> values = new ArrayList<String>();

            boolean encoded = metadata.queryMapEncoded();
            Object currValue = currEntry.getValue();
            if (currValue instanceof Iterable<?>) {
                Iterator<?> iter = ((Iterable<?>) currValue).iterator();
                while (iter.hasNext()) {
                    Object nextObject = iter.next();
                    if (nextObject == null) {
                        values.add(null);
                    } else {
                        values.add(encoded ? nextObject.toString() : UriUtils.encode(nextObject.toString()));
                    }
                    /*
                     * values.add(nextObject == null ? null : encoded ? nextObject.toString() :
                     * UriUtils.encode(nextObject.toString()));
                     */
                }
            } else {
                if (currValue == null) {
                    values.add(null);
                } else {
                    values.add(encoded ? currValue.toString() : UriUtils.encode(currValue.toString()));
                }
                /*
                 * values.add(currValue == null ? null : encoded ? currValue.toString() :
                 * UriUtils.encode(currValue.toString()));
                 */
            }

            mutable.query(encoded ? currEntry.getKey() : UriUtils.encode(currEntry.getKey()), values);
        }
        return mutable;
    }

    protected RequestTemplate resolve(Object[] argv, RequestTemplate mutable, Map<String, Object> variables,
        MethodMetadata methodMetadata) {
        return mutable.resolve(variables);
    }
}