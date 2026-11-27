package com.demo.risk.fieldcomplete.model;

import javax.validation.constraints.NotNull;
import java.util.Map;

public class ExecuteRequest {

    /** 已保存脚本的 id，与 scriptContent 二选一，同时给时优先 scriptContent */
    private String scriptId;

    /** 内联脚本内容，直接执行不入库 */
    private String scriptContent;

    @NotNull(message = "input 不能为空")
    private Map<String, Object> input;

    public String getScriptId() {
        return scriptId;
    }

    public void setScriptId(String scriptId) {
        this.scriptId = scriptId;
    }

    public String getScriptContent() {
        return scriptContent;
    }

    public void setScriptContent(String scriptContent) {
        this.scriptContent = scriptContent;
    }

    public Map<String, Object> getInput() {
        return input;
    }

    public void setInput(Map<String, Object> input) {
        this.input = input;
    }
}
