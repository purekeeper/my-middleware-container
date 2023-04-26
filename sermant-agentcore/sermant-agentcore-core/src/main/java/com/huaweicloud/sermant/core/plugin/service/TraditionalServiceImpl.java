package com.huaweicloud.sermant.core.plugin.service;

import com.huaweicloud.sermant.core.plugin.PluginManager;
import com.huaweicloud.sermant.core.plugin.config.PluginMeta;
import com.huaweicloud.sermant.core.utils.JarFileUtils;
import com.huaweicloud.sermant.core.utils.StringUtils;



import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.jar.JarFile;

/**
 * @author jeff.yang
 * @date 2023-04-24 17:46:27
 **/
public class TraditionalServiceImpl implements PluginService {
    private String pluginPath;
    private ClassLoader classLoader;
    private Instrumentation instrumentation;
    private PluginMeta pluginMeta;

    public TraditionalServiceImpl(String pluginPath, ClassLoader classLoader, Instrumentation instrumentation, PluginMeta pluginMeta) {
        this.pluginPath = pluginPath;
        this.classLoader = classLoader;
        this.instrumentation = instrumentation;
        this.pluginMeta = pluginMeta;
    }

    @Override
    public void start() {
        // 加载起来 agent jar，并反射调用启动
        JarFile agentJarFile = PluginManager.getAgentJarFile(pluginPath);
        try {
            String className = JarFileUtils.getManifestAttr(agentJarFile, "Premain-Class").toString();
            String methodName = "premain";
            if (StringUtils.isEmpty(className)) {
                className = JarFileUtils.getManifestAttr(agentJarFile, "Agent-Class").toString();
                methodName = "agentmain";
            }
            Class<?> clazz = this.classLoader.loadClass(className);
            // 反射调用启动
            Method method = clazz.getMethod(methodName, String.class, Instrumentation.class);
            method.invoke(null, this.pluginMeta.getAgentArgs(), this.instrumentation);
        } catch (Throwable e) {
            throw new RuntimeException("start error, agent jar::" + agentJarFile, e);
        }
    }
}
