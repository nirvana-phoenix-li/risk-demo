package com.demo.risk.fieldcomplete;

import com.demo.risk.common.Result;
import com.demo.risk.fieldcomplete.model.ExecuteRequest;
import com.demo.risk.fieldcomplete.model.ScriptInfo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/field-complete")
public class FieldCompleteController {

    private final ScriptStore scriptStore;
    private final GroovyScriptEngine scriptEngine;

    public FieldCompleteController(ScriptStore scriptStore, GroovyScriptEngine scriptEngine) {
        this.scriptStore = scriptStore;
        this.scriptEngine = scriptEngine;
    }

    @PostMapping("/execute")
    public Result<Map<String, Object>> execute(@Validated @RequestBody ExecuteRequest request) {
        String cacheKey;
        String scriptContent;

        if (request.getScriptContent() != null && !request.getScriptContent().isEmpty()) {
            cacheKey = "inline@" + request.getScriptContent().hashCode();
            scriptContent = request.getScriptContent();
        } else if (request.getScriptId() != null && !request.getScriptId().isEmpty()) {
            ScriptInfo info = scriptStore.require(request.getScriptId());
            cacheKey = "script@" + info.getScriptId() + "@" + info.getContent().hashCode();
            scriptContent = info.getContent();
        } else {
            return Result.fail(400, "scriptId 与 scriptContent 至少提供一个");
        }

        Map<String, Object> input = new LinkedHashMap<>(request.getInput());
        Map<String, Object> completed = scriptEngine.execute(cacheKey, scriptContent, input);
        return Result.ok(completed);
    }

    @PostMapping("/scripts")
    public Result<ScriptInfo> saveScript(@Validated @RequestBody ScriptInfo info) {
        scriptStore.save(info);
        return Result.ok(info);
    }

    @GetMapping("/scripts")
    public Result<List<ScriptInfo>> listScripts() {
        return Result.ok(scriptStore.list());
    }

    @GetMapping("/scripts/{scriptId}")
    public Result<ScriptInfo> getScript(@PathVariable String scriptId) {
        return Result.ok(scriptStore.require(scriptId));
    }

    @DeleteMapping("/scripts/{scriptId}")
    public Result<String> deleteScript(@PathVariable String scriptId) {
        ScriptInfo removed = scriptStore.delete(scriptId);
        if (removed == null) {
            return Result.fail(404, "脚本不存在: " + scriptId);
        }
        return Result.ok("已删除: " + scriptId);
    }
}
