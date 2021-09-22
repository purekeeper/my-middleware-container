package com.huawei.apm.premain;

import com.huawei.apm.bootstrap.definition.EnhanceDefinition;
import com.huawei.apm.bootstrap.definition.MethodInterceptPoint;
import com.huawei.apm.bootstrap.interceptors.ConstructorInterceptor;
import com.huawei.apm.bootstrap.interceptors.InstanceMethodInterceptor;
import com.huawei.apm.bootstrap.interceptors.StaticMethodInterceptor;
import com.huawei.apm.common.OverrideArgumentsCall;
import com.huawei.apm.enhance.enhancer.ConstructorEnhancer;
import com.huawei.apm.enhance.enhancer.InstanceMethodEnhancer;
import com.huawei.apm.enhance.enhancer.StaticMethodEnhancer;
import com.huawei.apm.premain.boot.PluginServiceManager;
import com.lubanops.apm.bootstrap.Listener;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.bind.annotation.Morph;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.huawei.apm.enhance.InterceptorLoader.getInterceptors;

/**
 * 多次增强Transformer
 */
public class Transformer implements AgentBuilder.Transformer {

    private final EnhanceDefinitionLoader enhanceDefinitionLoader;

    public Transformer(EnhanceDefinitionLoader enhanceDefinitionLoader) {
        this.enhanceDefinitionLoader = enhanceDefinitionLoader;
    }

    @Override
    public DynamicType.Builder<?> transform(final DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
        final List<Listener> nameListeners = enhanceDefinitionLoader.findNameListeners(typeDescription);
        final List<EnhanceDefinition> definitions = enhanceDefinitionLoader.findDefinitions(typeDescription);

        if (nameListeners.isEmpty() && definitions.isEmpty()) {
            return builder;
        }

        DynamicType.Builder<?> newBuilder = builder;
        for (Listener listener : nameListeners) {
            newBuilder = NamedListenerTransformer.getInstance()
                .enhanceNamedListener(listener, newBuilder, typeDescription, classLoader);
        }

        // 当前实现方式，新功能会覆盖同一个拦截点下的老功能增强器
        if (!definitions.isEmpty()) {
            newBuilder = enhanceMethods(definitions, newBuilder, typeDescription, classLoader);
        }
        // 初始化插件, 只会调用一次, 目的是使用增强类的类加载器对插件初始化, 这样可保证拦截器以及初始化的内容数据可共享
        PluginServiceManager.INSTANCE.init(classLoader);
        return BuilderHelpers.addEnhancedField(newBuilder);
    }

    private DynamicType.Builder<?> enhanceMethods(List<EnhanceDefinition> definitions,
            final DynamicType.Builder<?> builder,
            TypeDescription typeDescription,
            ClassLoader classLoader) {
        DynamicType.Builder<?> newBuilder = builder;

        List<MultiInterMethodHolder> methodHolders = getMethodsToBeEnhanced(definitions, typeDescription);
        for (MultiInterMethodHolder methodHolder : methodHolders) {
            if (methodHolder.isConstructor()) {
                newBuilder = enhanceConstructor(classLoader, newBuilder, methodHolder);
            } else if (methodHolder.isStatic()) {
                newBuilder = enhanceStaticMethod(classLoader, newBuilder, methodHolder);
            } else {
                newBuilder = enhanceInstanceMethod(classLoader, newBuilder, methodHolder);
            }
        }
        return newBuilder;
    }

    private DynamicType.Builder<?> enhanceInstanceMethod(ClassLoader classLoader,
            DynamicType.Builder<?> newBuilder,
            MultiInterMethodHolder methodHolder) {
        return newBuilder.method(methodHolder.getMatcher())
            .intercept(MethodDelegation.withDefaultConfiguration()
                .withBinders(Morph.Binder.install(OverrideArgumentsCall.class))
                .to(new InstanceMethodEnhancer(
                    getInterceptors(methodHolder.getInterceptors(), classLoader, InstanceMethodInterceptor.class))));
    }

    private DynamicType.Builder<?> enhanceStaticMethod(ClassLoader classLoader,
            DynamicType.Builder<?> newBuilder,
            MultiInterMethodHolder methodHolder) {
        return newBuilder.method(methodHolder.getMatcher())
            .intercept(MethodDelegation.withDefaultConfiguration()
                .withBinders(Morph.Binder.install(OverrideArgumentsCall.class))
                .to(new StaticMethodEnhancer(
                    getInterceptors(methodHolder.getInterceptors(), classLoader, StaticMethodInterceptor.class))));
    }

    private DynamicType.Builder<?> enhanceConstructor(ClassLoader classLoader,
            DynamicType.Builder<?> newBuilder,
            MultiInterMethodHolder methodHolder) {
        return newBuilder.method(methodHolder.getMatcher())
                .intercept(SuperMethodCall.INSTANCE.andThen(
                        MethodDelegation.withDefaultConfiguration().to(new ConstructorEnhancer(
                                getInterceptors(methodHolder.getInterceptors(), classLoader, ConstructorInterceptor.class)))));
    }

    private List<MultiInterMethodHolder> getMethodsToBeEnhanced(List<EnhanceDefinition> definitions,
            TypeDescription typeDescription) {
        // 构造每个拦截器的匹配条件
        Map<String, ElementMatcher.Junction<MethodDescription>> matcherGroupByInterceptor =
                new HashMap<String, ElementMatcher.Junction<MethodDescription>>();
        for (EnhanceDefinition definition : definitions) {
            MethodInterceptPoint[] interceptPoints = definition.getMethodInterceptPoints();
            for (MethodInterceptPoint interceptPoint : interceptPoints) {
                String interceptor = interceptPoint.getInterceptor();
                ElementMatcher.Junction<MethodDescription> matcher = matcherGroupByInterceptor.get(interceptor);
                if (matcher == null) {
                    matcher = ElementMatchers.none();
                }
                matcher = matcher.or(interceptPoint.getMatcher());
                matcherGroupByInterceptor.put(interceptor, matcher);
            }
        }

        MethodList<MethodDescription.InDefinedShape> declaredMethods = typeDescription.getDeclaredMethods();
        // 找出所有满足条件的方法以及其所对应的所有拦截器
        List<MultiInterMethodHolder> methodHolders = new LinkedList<MultiInterMethodHolder>();
        Set<Map.Entry<String, ElementMatcher.Junction<MethodDescription>>> interceptorMatcherEntries =
                matcherGroupByInterceptor.entrySet();
        for (MethodDescription.InDefinedShape method : declaredMethods) {
            Set<String> matchedInterceptors = new HashSet<String>();
            for (Map.Entry<String, ElementMatcher.Junction<MethodDescription>> entry : interceptorMatcherEntries) {
                if (entry.getValue().matches(method)) {
                    matchedInterceptors.add(entry.getKey());
                }
            }
            if (!matchedInterceptors.isEmpty()) {
                methodHolders.add(new MultiInterMethodHolder(method, matchedInterceptors.toArray(new String[0])));
            }
        }
        return methodHolders;
    }

    private static class MultiInterMethodHolder {

        private final MethodDescription.InDefinedShape method;

        private final String[] interceptors;

        public MultiInterMethodHolder(MethodDescription.InDefinedShape method, String[] interceptors) {
            this.method = method;
            this.interceptors = interceptors;
        }

        public ElementMatcher.Junction<MethodDescription> getMatcher() {
            return ElementMatchers.is(method);
        }

        public boolean isConstructor() {
            return method.isConstructor();
        }

        public boolean isStatic() {
            return method.isStatic();
        }

        public String[] getInterceptors() {
            return interceptors;
        }

    }
}