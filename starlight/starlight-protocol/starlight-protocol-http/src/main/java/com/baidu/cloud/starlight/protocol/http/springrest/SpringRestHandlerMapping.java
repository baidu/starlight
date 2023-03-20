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

import com.baidu.cloud.starlight.api.model.RpcRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.support.DefaultDataBinderFactory;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.ExpressionValueMethodArgumentResolver;
import org.springframework.web.method.annotation.RequestHeaderMapMethodArgumentResolver;
import org.springframework.web.method.annotation.RequestHeaderMethodArgumentResolver;
import org.springframework.web.method.annotation.RequestParamMapMethodArgumentResolver;
import org.springframework.web.method.annotation.RequestParamMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.MatrixVariableMapMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.MatrixVariableMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.PathVariableMapMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.PathVariableMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestAttributeMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestPartMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;
import org.springframework.web.servlet.mvc.method.annotation.ServletCookieValueMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.ServletRequestMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.ServletResponseMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.SessionAttributeMethodArgumentResolver;

import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TODO 重复测试所有可能发生的情况 Extend spring-mvc {@link RequestMappingHandlerMapping} to convert HttpRequest to
 * {@link RpcRequest}.
 * <ul>
 * <li>Step1: Store the mapping relationship between Method and RequestMappingInfo
 * {@link #createMapping(Class, Object)}</li>
 * <li>Step2: According to the mapping relationship, map HttpRequest to MethodHandler
 * {@link #handler(HttpServletRequest)}</li>
 * <li>Step3: Convert HttpRequst to method params
 * {@link #resolveArguments(HandlerMethod, HttpServletRequest, HttpServletResponse)}</li>
 * </ul>
 * Used in {@link SpringRestHttpDecoder}.
 *
 * @see SpringRestHttpDecoder#reverseConvertRequest Created by liuruisen on 2020/6/5.
 */
public class SpringRestHandlerMapping extends RequestMappingHandlerMapping {

    private static Logger logger = LoggerFactory.getLogger(SpringRestHandlerMapping.class);

    private static SpringRestHandlerMapping requestMappingHandlerMapping;

    private static final HandlerMethodArgumentResolverComposite argumentResolvers =
        new HandlerMethodArgumentResolverComposite();

    private static final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    private static final WebDataBinderFactory dataBinderFactory = new DefaultDataBinderFactory(null);

    private SpringRestHandlerMapping() {}

    public static SpringRestHandlerMapping getInstance() {
        synchronized (SpringRestHandlerMapping.class) {
            if (requestMappingHandlerMapping == null) {
                requestMappingHandlerMapping = new SpringRestHandlerMapping();
                argumentResolvers.addResolvers(getDefaultArgumentResolvers());
            }
        }
        return requestMappingHandlerMapping;
    }

    /**
     * Called in the initialization phase to establish the mapping relationship between request uri and Method
     *
     * @param serviceType
     * @param serviceObj
     */
    public void createMapping(Class serviceType, Object serviceObj) {
        if (serviceType != null) {
            Class<?> userType = ClassUtils.getUserClass(serviceType);
            Map<Method, RequestMappingInfo> methods = MethodIntrospector.selectMethods(userType,
                (MethodIntrospector.MetadataLookup<RequestMappingInfo>) method -> {
                    try {
                        return getMappingForMethod(method, userType);
                    } catch (Throwable ex) {
                        throw new IllegalStateException(
                            "Invalid mapping on handler class [" + userType.getName() + "]: " + method, ex);
                    }
                });
            logger.debug(methods.size() + " request handler methods found on " + userType + ": " + methods);
            methods.forEach((method, mapping) -> {
                Method invocableMethod = AopUtils.selectInvocableMethod(method, userType);
                registerHandlerMethod(serviceObj, invocableMethod, mapping);
            });
        }
    }

    /**
     * Get the target HandlerMethod mapping according to {@link HttpServletRequest} Since
     * {@link RequestMappingHandlerMapping} is using {@link HttpServletRequest}, we also use
     *
     * @param request
     * @return
     */
    public HandlerMethod handler(HttpServletRequest request) throws Exception {
        return getHandlerInternal(request);
    }

    /**
     * Extract parameters from {@link HttpServletRequest}.
     *
     * @param method
     * @param request
     * @param response
     * @return param values for method
     * @throws Exception
     */
    public Object[] resolveArguments(HandlerMethod method, HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        ServletWebRequest webRequest = new ServletWebRequest(request, response);

        return getMethodArgumentValues(webRequest, method);
    }

    /**
     * Get the method argument values for the current request. Migrant from spring-mvc
     */
    private Object[] getMethodArgumentValues(NativeWebRequest request, HandlerMethod method) throws Exception {

        MethodParameter[] parameters = method.getMethodParameters();
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            MethodParameter parameter = parameters[i];
            parameter.initParameterNameDiscovery(SpringRestHandlerMapping.parameterNameDiscoverer);
            if (SpringRestHandlerMapping.argumentResolvers.supportsParameter(parameter)) {
                try {
                    args[i] = SpringRestHandlerMapping.argumentResolvers.resolveArgument(parameter, null, request,
                        dataBinderFactory);
                    continue;
                } catch (Exception ex) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Failed to resolve", ex);
                    }
                    throw ex;
                }
            }
            if (args[i] == null) {
                throw new IllegalStateException(
                    "Could not resolve method parameter at index " + parameter.getParameterIndex() + " in "
                        + parameter.getExecutable().toGenericString() + ": " + "No suitable resolver for");
            }
        }
        return args;
    }

    /**
     * Return the list of argument resolvers to use including built-in resolvers and custom resolvers provided via
     * {@link #}.
     */
    private static List<HandlerMethodArgumentResolver> getDefaultArgumentResolvers() {
        List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();

        // Annotation-based argument resolution
        // @RequestParam
        resolvers.add(new RequestParamMethodArgumentResolver(null, false));
        resolvers.add(new RequestParamMapMethodArgumentResolver()); // @RequestParam map
        resolvers.add(new PathVariableMethodArgumentResolver()); // @PathVariable
        resolvers.add(new PathVariableMapMethodArgumentResolver()); // @PathVariable map
        resolvers.add(new MatrixVariableMethodArgumentResolver()); // @MatrixVariable (not support)
        resolvers.add(new MatrixVariableMapMethodArgumentResolver()); // @MatrixVariable map (not support)

        StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter();
        stringHttpMessageConverter.setWriteAcceptCharset(false); // see SPR-7316
        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>(2);
        messageConverters.add(new BytesHttpMessageConverter());
        messageConverters.add(stringHttpMessageConverter);

        resolvers.add(new RequestResponseBodyMethodProcessor(messageConverters)); // @RequestBody
        resolvers.add(new RequestPartMethodArgumentResolver(messageConverters)); // @RequestPart (not support)
        resolvers.add(new RequestHeaderMethodArgumentResolver(null)); // @RequestHeader
        resolvers.add(new RequestHeaderMapMethodArgumentResolver()); // @RequestHeader map
        resolvers.add(new ServletCookieValueMethodArgumentResolver(null)); //
        resolvers.add(new ExpressionValueMethodArgumentResolver(null));
        resolvers.add(new SessionAttributeMethodArgumentResolver());
        resolvers.add(new RequestAttributeMethodArgumentResolver()); // @RequestAttribute

        // servlet request and servlet response
        resolvers.add(new ServletRequestMethodArgumentResolver());
        resolvers.add(new ServletResponseMethodArgumentResolver());

        // Catch-all
        resolvers.add(new RequestParamMethodArgumentResolver(null, true));
        return resolvers;
    }

    /**
     * Change the scope of the method to public
     * 
     * @param method
     * @param handlerType
     * @return
     */
    @Override
    public RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
        Map<RequestMappingInfo, HandlerMethod> requestMappingInfoHandlerMethodMap = getHandlerMethods();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : requestMappingInfoHandlerMethodMap.entrySet()) {
            if (entry.getValue().getMethod().equals(method)) {
                return entry.getKey();
            }
        }
        return super.getMappingForMethod(method, handlerType);
    }
}
