package com.demo.risk.fieldcomplete;

import com.demo.risk.fieldcomplete.model.ScriptInfo;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ScriptStore {

    private final Map<String, ScriptInfo> scripts = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            save(loadPreloaded("demo-basic", "基础字段补全(演示)"));
            save(loadPreloaded("demo-mask", "敏感字段脱敏(演示)"));
        } catch (IOException e) {
            throw new IllegalStateException("预置脚本加载失败", e);
        }
    }

    private ScriptInfo loadPreloaded(String scriptId, String name) throws IOException {
        ClassPathResource resource = new ClassPathResource("scripts/" + scriptId + ".groovy");
        try (InputStream in = resource.getInputStream()) {
            String content = new String(StreamUtils.copyToByteArray(in), StandardCharsets.UTF_8);
            ScriptInfo info = new ScriptInfo();
            info.setScriptId(scriptId);
            info.setName(name);
            info.setContent(content);
            return info;
        }
    }

    public void save(ScriptInfo info) {
        scripts.put(info.getScriptId(), info);
    }

    public ScriptInfo get(String scriptId) {
        return scripts.get(scriptId);
    }

    public ScriptInfo require(String scriptId) {
        ScriptInfo info = scripts.get(scriptId);
        if (info == null) {
            throw new GroovyScriptException("脚本不存在: " + scriptId);
        }
        return info;
    }

    public ScriptInfo delete(String scriptId) {
        return scripts.remove(scriptId);
    }

    public List<ScriptInfo> list() {
        return new ArrayList<>(scripts.values());
    }
}
