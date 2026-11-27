package com.demo.risk.fieldcomplete;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;
import org.codehaus.groovy.control.CompilationFailedException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 脚本约定：绑定变量 input 为入参 JSON 解析出的 Map，
 * 脚本 return 一个 Map，其字段会合并回 input。
 */
@Component
public class GroovyScriptEngine {

    private final GroovyClassLoader classLoader = new GroovyClassLoader(GroovyScriptEngine.class.getClassLoader());
    private final Map<String, Class<?>> compiledCache = new ConcurrentHashMap<>();

    public Map<String, Object> execute(String cacheKey, String scriptContent, Map<String, Object> input) {
        Script script = createScript(cacheKey, scriptContent);

        Binding binding = new Binding();
        binding.setVariable("input", input);
        script.setBinding(binding);

        Object returnValue;
        try {
            returnValue = script.run();
        } catch (Exception e) {
            throw new GroovyScriptException("脚本运行失败: " + rootMessage(e), e);
        }

        if (returnValue instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) returnValue).entrySet()) {
                input.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return input;
    }

    private Script createScript(String cacheKey, String scriptContent) {
        try {
            Class<?> clazz = compiledCache.computeIfAbsent(cacheKey, k -> compile(scriptContent));
            return (Script) clazz.getDeclaredConstructor().newInstance();
        } catch (GroovyScriptException e) {
            throw e;
        } catch (Exception e) {
            throw new GroovyScriptException("脚本初始化失败: " + rootMessage(e), e);
        }
    }

    private Class<?> compile(String scriptContent) {
        try {
            return classLoader.parseClass(scriptContent);
        } catch (CompilationFailedException e) {
            throw new GroovyScriptException("脚本语法错误: " + rootMessage(e));
        }
    }

    private String rootMessage(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();
    }
}
