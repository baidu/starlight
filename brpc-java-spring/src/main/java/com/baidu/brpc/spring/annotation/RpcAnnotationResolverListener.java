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
package com.baidu.brpc.spring.annotation;

import java.util.List;

import com.baidu.brpc.spring.RpcProxyFactoryBean;

/**
 * Listener class for {@link RpcAnnotationResolver} while doing annotation parser action.
 *
 * @author xiemalin
 * @since 3.4.1
 */
public interface RpcAnnotationResolverListener {

	/**
	 * This method will called after {@link RpcExporter} annotation parsed.
	 *
	 * @param rpcExporter {@link RpcExporter} annotation
	 * @param port real port
	 * @param bean target bean
	 * @param registerServices registered service list
	 */
	void onRpcExporterAnnotationParsered(RpcExporter rpcExporter, int port, Object bean, List<Object> registerServices);

	
	/**
	 * Destroy.
	 */
	void destroy();


	/**
	 * This method will called after {@link RpcProxy} annotation parsed.
	 *
	 * @param rpcProxy {@link RpcProxy} annotation
	 * @param newRpcProxyFactoryBean a new created {@link RpcProxyFactoryBean}
	 * @param object target PROXY bean for this {@link RpcProxy} annotation delegated
	 */
	void onRpcProxyAnnotationParsed(RpcProxy rpcProxy, RpcProxyFactoryBean newRpcProxyFactoryBean, Object object);
	
	
	
}

