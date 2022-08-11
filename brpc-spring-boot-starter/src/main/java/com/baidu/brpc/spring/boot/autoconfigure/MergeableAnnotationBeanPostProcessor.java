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
package com.baidu.brpc.spring.boot.autoconfigure;

import com.baidu.brpc.spring.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Common annotation bean post processor. it uses {@link com.baidu.brpc.spring.annotation.AnnotationParserCallback}<br>
 * interface to define specified {@link java.lang.annotation.Annotation} then recognize the Class to do <br>
 * bean define action
 *
 * @author zhangzicheng
 * @see com.baidu.brpc.spring.annotation.AnnotationParserCallback
 */
public class MergeableAnnotationBeanPostProcessor extends CommonAnnotationBeanPostProcessor {
    @Override
    protected Annotation getAnnotationInClass(Class clazz, Class<? extends Annotation> annotation) {
        return AnnotatedElementUtils.findMergedAnnotation(clazz, annotation);
    }
    
    @Override
    protected Annotation getAnnotationInMethod(Method method, Class<? extends Annotation> anno) {
        return AnnotatedElementUtils.findMergedAnnotation(method, anno);
    }
    
    @Override
    protected Annotation getAnnotationInField(Field field, Class<? extends Annotation> anno) {
        return AnnotatedElementUtils.findMergedAnnotation(field, anno);
    }
}