package com.tiny.mousse.annotation;

import com.tiny.mousse.rpc.RpcInvoker;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.annotation.AnnotationAttributes;
import com.alibaba.spring.util.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.alibaba.spring.util.AnnotationUtils.getAttribute;
import static com.alibaba.spring.util.AnnotationUtils.getAttributes;
import static org.springframework.aop.support.AopUtils.getTargetClass;
import static org.springframework.core.BridgeMethodResolver.findBridgedMethod;
import static org.springframework.core.BridgeMethodResolver.isVisibilityBridgeMethodPair;
import static org.springframework.core.GenericTypeResolver.resolveTypeArgument;
import static org.springframework.util.StringUtils.hasText;

/**
 *
 * @author iterators
 * @since 2023/03/05
 */
public class ReferenceAnnotationBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter implements
        ApplicationContextAware, EnvironmentAware {

    public static final String BEAN_NAME = "referenceAnnotationBeanPostProcessor";

    private ApplicationContext applicationContext;

    private Environment environment;

    private boolean classValuesAsString = true;

    private boolean nestedAnnotationsAsMap = true;

    private boolean ignoreDefaultValue = true;

    private boolean tryMergedAnnotation = true;

    private final ConcurrentMap<String, ReferenceAnnotationBeanPostProcessor.AnnotatedInjectionMetadata> injectionMetadataCache =
            new ConcurrentHashMap<>(32);

    private final ConcurrentMap<String, Object> injectedObjectsCache = new ConcurrentHashMap<>(32);

    protected final Class<? extends Annotation>[] getAnnotationTypes() {
        return new Class[]{RpcReference.class};
    }



    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    protected Environment getEnvironment() {
        return environment;
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) throws BeansException {
        InjectionMetadata metadata = findInjectionMetadata(beanName, bean.getClass(), pvs);
        try {
            metadata.inject(bean, beanName, pvs);
        } catch (BeanCreationException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new BeanCreationException(beanName, "Injection of @" + RpcReference.class.getSimpleName()
                    + " dependencies is failed", ex);
        }
        return pvs;
    }

    private InjectionMetadata findInjectionMetadata(String beanName, Class<?> clazz, PropertyValues pvs) {
        // Fall back to class name as cache key, for backwards compatibility with custom callers.
        String cacheKey = (StringUtils.hasLength(beanName) ? beanName : clazz.getName());

        ReferenceAnnotationBeanPostProcessor.AnnotatedInjectionMetadata metadata = this.injectionMetadataCache.get(cacheKey);
        if (Objects.isNull(metadata)) {
            synchronized (this.injectionMetadataCache) {
                metadata = this.injectionMetadataCache.get(cacheKey);
                if (Objects.isNull(metadata)) {
                    try {
                        metadata = buildAnnotatedMetadata(clazz);
                        this.injectionMetadataCache.put(cacheKey, metadata);
                    } catch (NoClassDefFoundError err) {
                        throw new IllegalStateException("Failed to introspect object class [" + clazz.getName() +
                                "] for annotation metadata: could not find class that it depends on", err);
                    }
                }
            }
        }
        return metadata;
    }

    private ReferenceAnnotationBeanPostProcessor.AnnotatedInjectionMetadata buildAnnotatedMetadata(final Class<?> beanClass) {
        Collection<ReferenceAnnotationBeanPostProcessor.AnnotatedFieldElement> fieldElements = findFieldAnnotationMetadata(beanClass);
        Collection<ReferenceAnnotationBeanPostProcessor.AnnotatedMethodElement> methodElements = findAnnotatedMethodMetadata(beanClass);
        return new ReferenceAnnotationBeanPostProcessor.AnnotatedInjectionMetadata(beanClass, fieldElements, methodElements);
    }

    private List<ReferenceAnnotationBeanPostProcessor.AnnotatedFieldElement> findFieldAnnotationMetadata(final Class<?> beanClass) {

        final List<ReferenceAnnotationBeanPostProcessor.AnnotatedFieldElement> elements = new LinkedList<>();

        ReflectionUtils.doWithFields(beanClass, new ReflectionUtils.FieldCallback() {
            @Override
            public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {

                for (Class<? extends Annotation> annotationType : getAnnotationTypes()) {

                    AnnotationAttributes attributes = doGetAnnotationAttributes(field, annotationType);

                    if (attributes != null) {

                        if (Modifier.isStatic(field.getModifiers())) {
                            return;
                        }

                        elements.add(new AnnotatedFieldElement(field, attributes));
                    }
                }
            }
        });

        return elements;

    }

    protected AnnotationAttributes doGetAnnotationAttributes(AnnotatedElement annotatedElement,
                                                             Class<? extends Annotation> annotationType) {
        return AnnotationUtils.getAnnotationAttributes(annotatedElement, annotationType, getEnvironment(),
                classValuesAsString, nestedAnnotationsAsMap, ignoreDefaultValue, tryMergedAnnotation);
    }

    private List<ReferenceAnnotationBeanPostProcessor.AnnotatedMethodElement> findAnnotatedMethodMetadata(final Class<?> beanClass) {

        final List<ReferenceAnnotationBeanPostProcessor.AnnotatedMethodElement> elements = new LinkedList<>();

        ReflectionUtils.doWithMethods(beanClass, new ReflectionUtils.MethodCallback() {
            @Override
            public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {

                Method bridgedMethod = findBridgedMethod(method);

                if (!isVisibilityBridgeMethodPair(method, bridgedMethod)) {
                    return;
                }


                for (Class<? extends Annotation> annotationType : getAnnotationTypes()) {

                    AnnotationAttributes attributes = doGetAnnotationAttributes(bridgedMethod, annotationType);

                    if (attributes != null && method.equals(ClassUtils.getMostSpecificMethod(method, beanClass))) {
                        if (Modifier.isStatic(method.getModifiers())) {
                            return;
                        }
                        if (method.getParameterTypes().length == 0) {

                        }
                        PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, beanClass);
                        elements.add(new AnnotatedMethodElement(method, pd, attributes));
                    }
                }
            }
        });

        return elements;
    }

    private static <T> Collection<T> combine(Collection<? extends T>... elements) {
        List<T> allElements = new ArrayList<T>();
        for (Collection<? extends T> e : elements) {
            allElements.addAll(e);
        }
        return allElements;
    }


    private class AnnotatedInjectionMetadata extends InjectionMetadata {

        private final Collection<ReferenceAnnotationBeanPostProcessor.AnnotatedFieldElement> fieldElements;

        private final Collection<ReferenceAnnotationBeanPostProcessor.AnnotatedMethodElement> methodElements;

        public AnnotatedInjectionMetadata(Class<?> targetClass, Collection<ReferenceAnnotationBeanPostProcessor.AnnotatedFieldElement> fieldElements,
                                          Collection<ReferenceAnnotationBeanPostProcessor.AnnotatedMethodElement> methodElements) {
            super(targetClass, combine(fieldElements, methodElements));
            this.fieldElements = fieldElements;
            this.methodElements = methodElements;
        }

        public Collection<ReferenceAnnotationBeanPostProcessor.AnnotatedFieldElement> getFieldElements() {
            return fieldElements;
        }

        public Collection<ReferenceAnnotationBeanPostProcessor.AnnotatedMethodElement> getMethodElements() {
            return methodElements;
        }
    }

    public class AnnotatedFieldElement extends InjectionMetadata.InjectedElement {

        private final Field field;

        private final AnnotationAttributes attributes;

        private volatile Object bean;

        protected AnnotatedFieldElement(Field field, AnnotationAttributes attributes) {
            super(field, null);
            this.field = field;
            this.attributes = attributes;
        }

        @Override
        protected void inject(Object bean, String beanName, PropertyValues pvs) throws Throwable {

            Class<?> injectedType = resolveInjectedType(bean, field);

            Object injectedObject = getInjectedObject(attributes, bean, beanName, injectedType, this);

            ReflectionUtils.makeAccessible(field);

            field.set(bean, injectedObject);

        }

        private Class<?> resolveInjectedType(Object bean, Field field) {
            Type genericType = field.getGenericType();
            if (genericType instanceof Class) { // Just a normal Class
                return field.getType();
            } else { // GenericType
                return resolveTypeArgument(getTargetClass(bean), field.getDeclaringClass());
            }
        }
    }

    private class AnnotatedMethodElement extends InjectionMetadata.InjectedElement {

        private final Method method;

        private final AnnotationAttributes attributes;

        private volatile Object object;

        protected AnnotatedMethodElement(Method method, PropertyDescriptor pd, AnnotationAttributes attributes) {
            super(method, pd);
            this.method = method;
            this.attributes = attributes;
        }

        @Override
        protected void inject(Object bean, String beanName, PropertyValues pvs) throws Throwable {

            Class<?> injectedType = pd.getPropertyType();

            Object injectedObject = getInjectedObject(attributes, bean, beanName, injectedType, this);

            ReflectionUtils.makeAccessible(method);

            method.invoke(bean, injectedObject);

        }

    }

    protected Object getInjectedObject(AnnotationAttributes attributes, Object bean, String beanName, Class<?> injectedType,
                                       InjectionMetadata.InjectedElement injectedElement) throws Exception {

        String cacheKey = buildInjectedObjectCacheKey(attributes, bean, beanName, injectedType, injectedElement);

        Object injectedObject = injectedObjectsCache.get(cacheKey);

        if (injectedObject == null) {
            injectedObject = doGetInjectedBean(attributes, bean, beanName, injectedType, injectedElement);
            // Customized inject-object if necessary
            injectedObjectsCache.putIfAbsent(cacheKey, injectedObject);
        }

        return injectedObject;

    }


    protected String buildInjectedObjectCacheKey(AnnotationAttributes attributes, Object bean, String beanName,
                                                 Class<?> injectedType, InjectionMetadata.InjectedElement injectedElement) {
        return buildReferencedBeanName(attributes, injectedType) +
                "#source=" + (injectedElement.getMember()) +
                "#attributes=" + getAttributes(attributes, getEnvironment());
    }

    private String buildReferencedBeanName(AnnotationAttributes attributes, Class<?> serviceInterfaceType) {

        return serviceInterfaceType.getName();
    }

    protected Object doGetInjectedBean(AnnotationAttributes attributes, Object bean, String beanName, Class<?> injectedType,
                                       InjectionMetadata.InjectedElement injectedElement) throws Exception {

        String referencedBeanName = buildReferencedBeanName(attributes, injectedType);

        String referenceBeanName = getReferenceBeanName(attributes, injectedType);

        // referencedBeanNameIdx.computeIfAbsent(referencedBeanName, k -> new TreeSet<String>()).add(referenceBeanName);
        //
        // ReferenceBean referenceBean = buildReferenceBeanIfAbsent(referenceBeanName, attributes, injectedType);
        //
        // boolean localServiceBean = isLocalServiceBean(referencedBeanName, referenceBean, attributes);
        //
        // prepareReferenceBean(referencedBeanName, referenceBean, localServiceBean);
        //
        // registerReferenceBean(referencedBeanName, referenceBean, localServiceBean, referenceBeanName);
        //
        // cacheInjectedReferenceBean(referenceBean, injectedElement);

        // 生成代理对象
        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{injectedType}, new RpcInvoker(applicationContext));
    }

    private String getReferenceBeanName(AnnotationAttributes attributes, Class<?> interfaceClass) {
        // id attribute appears since 2.7.3
        String beanName = getAttribute(attributes, "id");
        if (!hasText(beanName)) {
            beanName = generateReferenceBeanName(attributes, interfaceClass);
        }
        return beanName;
    }

    private String generateReferenceBeanName(AnnotationAttributes attributes, Class<?> interfaceClass) {
        StringBuilder beanNameBuilder = new StringBuilder("@Reference");

        if (!attributes.isEmpty()) {
            beanNameBuilder.append('(');
            beanNameBuilder.setCharAt(beanNameBuilder.lastIndexOf(","), ')');
        }

        beanNameBuilder.append(" ").append(interfaceClass.getName());

        return beanNameBuilder.toString();
    }

}
