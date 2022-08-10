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

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import com.baidu.brpc.spring.PlaceholderResolver;
import com.baidu.brpc.spring.PropertyPlaceholderConfigurerTool;

/**
 * Common annotation bean post processor. it uses {@link AnnotationParserCallback}<br>
 * interface to define specified {@link Annotation} then recognize the Class to do <br>
 * bean define action
 *
 * @author xiemalin
 * @see AnnotationParserCallback
 */
public class CommonAnnotationBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter
        implements MergedBeanDefinitionPostProcessor, PriorityOrdered, BeanFactoryAware, DisposableBean,
        InitializingBean, ApplicationListener<ApplicationEvent> {

    /** log this class. todo why not slf4j */
    protected static final Log LOGGER = LogFactory.getLog(AutowiredAnnotationBeanPostProcessor.class);

    /** call back class for {@link AnnotationParserCallback}. */
    private AnnotationParserCallback callback;

    /**
     * start lowest order in spring container.
     */
    private int order = Ordered.LOWEST_PRECEDENCE - 3;

    /** {@link ConfigurableListableBeanFactory} instance. */
    private DefaultListableBeanFactory beanFactory;

    /** all injected meta data. */
    private final Map<Class<?>, InjectionMetadata> injectionMetadataCache =
            new ConcurrentHashMap<Class<?>, InjectionMetadata>();

    /** loaded property instance. */
    private Properties propertyResource;

    /** to support placeholder. */
    private PlaceholderResolver resolver;

    /**
     * management function to register all resolved bean has target annotation info.
     */
    private Vector<BeanInfo> typeAnnotationedBeans = new Vector<BeanInfo>();

    /** status to control start only once. */
    private AtomicBoolean started = new AtomicBoolean(false);

    /**
     * Gets the call back class for {@link AnnotationParserCallback}.
     *
     * @return the call back class for {@link AnnotationParserCallback}
     */
    private AnnotationParserCallback getCallback() {
        return callback;
    }

    /**
     * Sets the call back class for {@link AnnotationParserCallback}.
     *
     * @param callback the new call back class for {@link AnnotationParserCallback}
     */
    public void setCallback(AnnotationParserCallback callback) {
        this.callback = callback;
    }

    /**
     * Sets the start lowest order in spring container.
     *
     * @param order the new start lowest order in spring container
     */
    public void setOrder(int order) {
        this.order = order;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.core.Ordered#getOrder()
     */
    public int getOrder() {
        return this.order;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
     */
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
            throw new IllegalArgumentException(
                    "CommonAnnotationBeanPostProcessor requires a ConfigurableListableBeanFactory");
        }
        this.beanFactory = (DefaultListableBeanFactory) beanFactory;
    }

    /**
     * To parse all initialized bean from spring which has target annotation matches.
     * 
     * @param bean original bean initialize by spring
     * @param beanName bean name defined in spring
     * @return wrapped object by {@link AnnotationParserCallback}
     * @throws BeansException in case of initialization errors
     */
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (getCallback() == null) {
            return super.postProcessBeforeInitialization(bean, beanName);
        }

        Class clazz = bean.getClass();
        Class<? extends Annotation> annotation;
        annotation = getCallback().getTypeAnnotation();
        if (annotation == null) {
            return bean;
        }
        Annotation a = clazz.getAnnotation(annotation);
        if (a == null) {
            return bean;
        }
        BeanInfo beanInfo = new BeanInfo(beanName, a);
        typeAnnotationedBeans.add(beanInfo);
        return getCallback().annotationAtType(a, bean, beanName, beanFactory);
    }

    /**
     * Post-process the given merged bean definition for the specified bean.
     * 
     * @param beanDefinition the merged bean definition for the bean
     * @param beanType the actual type of the managed bean instance
     * @param beanName the name of the bean
     */
    public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class beanType, String beanName) {
        List<Class<? extends Annotation>> annotations = getCallback().getMethodFieldAnnotation();
        if (beanType != null && getCallback() != null && annotations != null) {
            InjectionMetadata metadata = findAnnotationMetadata(beanType, annotations);
            metadata.checkConfigMembers(beanDefinition);
        }
    }

    /**
     * Post-process the given property values before the factory applies them to the given bean. Allows for checking
     * whether all dependencies have been satisfied, for example based on a "Required" annotation on bean property
     * setters.
     * <p>
     * Also allows for replacing the property values to apply, typically through creating a new MutablePropertyValues
     * instance based on the original PropertyValues, adding or removing specific values.
     *
     * @param pvs the property values that the factory is about to apply (never {@code null})
     * @param pds the relevant property descriptors for the target bean (with ignored dependency types - which the
     *            factory handles specifically - already filtered out)
     * @param bean the bean instance created, but whose properties have not yet been set
     * @param beanName the name of the bean
     * @return the actual property values to apply to to the given bean (can be the passed-in PropertyValues instance),
     *         or {@code null} to skip property population
     * @throws BeansException the beans exception
     * @throws org.springframework.beans.BeansException in case of errors
     * @see org.springframework.beans.MutablePropertyValues
     */
    public PropertyValues postProcessPropertyValues(PropertyValues pvs, PropertyDescriptor[] pds, Object bean,
            String beanName) throws BeansException {
        List<Class<? extends Annotation>> annotations = getCallback().getMethodFieldAnnotation();
        if (getCallback() == null || annotations == null) {
            return pvs;
        }

        InjectionMetadata metadata = findAnnotationMetadata(bean.getClass(), annotations);
        try {
            metadata.inject(bean, beanName, pvs);
        } catch (Throwable ex) {
            throw new BeanCreationException(beanName, "Autowiring of methods failed", ex);
        }

        return pvs;
    }

    /**
     * To resolve annotation meta data info from target class.
     * 
     * @param clazz target class to resolve
     * @param annotation marked annotation type
     * @return all fields and methods matched target annotation type from specified class
     */
    private InjectionMetadata findAnnotationMetadata(final Class clazz,
            final List<Class<? extends Annotation>> annotation) {
        // Quick check on the concurrent map first, with minimal locking.
        InjectionMetadata metadata = this.injectionMetadataCache.get(clazz);
        if (metadata == null) {
            synchronized (this.injectionMetadataCache) {
                metadata = this.injectionMetadataCache.get(clazz);
                if (metadata == null) {
                    LinkedList<InjectionMetadata.InjectedElement> elements;
                    elements = new LinkedList<InjectionMetadata.InjectedElement>();
                    parseFields(clazz, annotation, elements);
                    parseMethods(clazz, annotation, elements);

                    metadata = new InjectionMetadata(clazz, elements);
                    this.injectionMetadataCache.put(clazz, metadata);
                }
            }
        }
        return metadata;
    }

    /**
     * To parse all method to find out annotation info.
     *
     * @param clazz target class
     * @param annotions the annotions
     * @param elements injected element of all methods
     */
    protected void parseMethods(final Class<?> clazz, final List<Class<? extends Annotation>> annotions,
            final LinkedList<InjectionMetadata.InjectedElement> elements) {
        ReflectionUtils.doWithMethods(clazz, new ReflectionUtils.MethodCallback() {
            public void doWith(Method method) {
                for (Class<? extends Annotation> anno : annotions) {
                    Annotation annotation = method.getAnnotation(anno);
                    if (annotation != null && method.equals(ClassUtils.getMostSpecificMethod(method, clazz))) {
                        if (Modifier.isStatic(method.getModifiers())) {
                            throw new IllegalStateException("Autowired annotation is not supported on static methods");
                        }
                        if (method.getParameterTypes().length == 0) {
                            throw new IllegalStateException(
                                    "Autowired annotation requires at least one argument: " + method);
                        }
                        PropertyDescriptor pd = BeanUtils.findPropertyForMethod(method);
                        elements.add(new AutowiredMethodElement(method, annotation, pd));
                    }
                }
            }
        });
    }

    /**
     * To parse all field to find out annotation info.
     *
     * @param clazz target class
     * @param annotations the annotations
     * @param elements injected element of all fields
     */
    protected void parseFields(final Class<?> clazz, final List<Class<? extends Annotation>> annotations,
            final LinkedList<InjectionMetadata.InjectedElement> elements) {
        ReflectionUtils.doWithFields(clazz, new ReflectionUtils.FieldCallback() {
            public void doWith(Field field) {
                for (Class<? extends Annotation> anno : annotations) {
                    Annotation annotation = field.getAnnotation(anno);
                    if (annotation != null) {
                        if (Modifier.isStatic(field.getModifiers())) {
                            throw new IllegalStateException("Autowired annotation is not supported on static fields");
                        }
                        elements.add(new AutowiredFieldElement(field, annotation));
                    }
                }
            }
        });
    }

    /**
     * Class representing injection information about an annotated field.
     */
    private class AutowiredFieldElement extends InjectionMetadata.InjectedElement {

        /** target annotation type. */
        private final Annotation annotation;

        /**
         * Constructor with field and annotation type.
         *
         * @param field field instance
         * @param annotation annotation type
         */
        public AutowiredFieldElement(Field field, Annotation annotation) {
            super(field, null);
            this.annotation = annotation;
        }

        @Override
        protected void inject(Object bean, String beanName, PropertyValues pvs) throws Throwable {
            Field field = (Field) this.member;
            try {
                ReflectionUtils.makeAccessible(field);
                Object value = field.get(bean);
                value = getCallback().annotationAtField(annotation, value, beanName, pvs, beanFactory, field);
                if (value != null) {
                    ReflectionUtils.makeAccessible(field);
                    field.set(bean, value);
                }
            } catch (Throwable ex) {
                throw new BeanCreationException("Could not autowire field: " + field, ex);
            }
        }
    }

    /**
     * Class representing injection information about an annotated method.
     */
    private class AutowiredMethodElement extends InjectionMetadata.InjectedElement {

        /** target annotation type. */
        private final Annotation annotation;

        /**
         * Constructor with method and annotation type.
         *
         * @param method method instance
         * @param annotation annotation type
         * @param pd {@link PropertyDescriptor} instance.
         */
        public AutowiredMethodElement(Method method, Annotation annotation, PropertyDescriptor pd) {
            super(method, pd);
            this.annotation = annotation;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.springframework.beans.factory.annotation.InjectionMetadata.InjectedElement#inject(java.lang.Object,
         * java.lang.String, org.springframework.beans.PropertyValues)
         */
        @Override
        protected void inject(Object bean, String beanName, PropertyValues pvs) throws Throwable {
            if (this.skip == null && this.pd != null && pvs != null && pvs.contains(this.pd.getName())) {
                // Explicit value provided as part of the bean definition.
                this.skip = Boolean.TRUE;
            }
            if (this.skip != null && this.skip.booleanValue()) {
                return;
            }
            Method method = (Method) this.member;
            try {
                Object[] arguments = null;

                Class[] paramTypes = method.getParameterTypes();
                arguments = new Object[paramTypes.length];

                for (int i = 0; i < arguments.length; i++) {
                    MethodParameter methodParam = new MethodParameter(method, i);
                    GenericTypeResolver.resolveParameterType(methodParam, bean.getClass());
                    arguments[i] =
                            getCallback().annotationAtMethod(annotation, bean, beanName, pvs, beanFactory, method);

                    if (arguments[i] == null) {
                        arguments = null;
                        break;
                    }
                }

                if (this.skip == null) {
                    if (this.pd != null && pvs instanceof MutablePropertyValues) {
                        ((MutablePropertyValues) pvs).registerProcessedProperty(this.pd.getName());
                    }
                    this.skip = Boolean.FALSE;
                }
                if (arguments != null) {
                    ReflectionUtils.makeAccessible(method);
                    method.invoke(bean, arguments);
                }
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            } catch (Throwable ex) {
                throw new BeanCreationException("Could not autowire method: " + method, ex);
            }
        }
    }

    /**
     * Invoke callback destroy method.
     *
     * @throws Exception in case of callback do destroy action error
     */
    public void destroy() throws Exception {
        if (getCallback() != null) {
            getCallback().destroy();
        }

    }

    /**
     * Initialize propertyResource instance load.
     *
     * @throws Exception in case of any error
     */
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(getCallback(), "property 'callback' must be set");

        propertyResource = PropertyPlaceholderConfigurerTool
                .getRegisteredPropertyResourceConfigurer((ConfigurableListableBeanFactory) beanFactory);
        if (resolver == null) {
            resolver = PropertyPlaceholderConfigurerTool.createPlaceholderParser(propertyResource);
        }

        if (getCallback() != null) {
            getCallback().setPlaceholderResolver(resolver);
        }

    }

    /**
     * Do annotation on class type resolve action after spring container started.
     * 
     * @param event spring application event
     */
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextStartedEvent || event instanceof ContextRefreshedEvent) {

            // only execute this method once. bug fix for ContextRefreshedEvent will invoke twice on spring MVC servlet
            //            if (started.compareAndSet(false, true)) {
            for (BeanInfo bean : typeAnnotationedBeans) {
                if (getCallback() != null) {
                    Object targetBean = beanFactory.getBean(bean.name);
                    getCallback().annotationAtTypeAfterStarted(bean.annotation, targetBean, bean.name, beanFactory);
                }
            }
            //            } else {
            //                LOGGER.warn("onApplicationEvent of application event [" + event
            //                        + "] ignored due to processor already started.");
            //            }

        }

    }

    /**
     * Wrapped bean info.
     *
     * @author xiemalin
     * @since 1.0.0.0
     */
    private static class BeanInfo {

        /** bean name. */
        private String name;

        /** annotation type. */
        private Annotation annotation;

        /**
         * Constructor with bean, name and annotation type.
         *
         * @param name bean name
         * @param annotation annotation type
         */
        public BeanInfo(String name, Annotation annotation) {
            super();
            this.name = name;
            this.annotation = annotation;
        }

    }

}