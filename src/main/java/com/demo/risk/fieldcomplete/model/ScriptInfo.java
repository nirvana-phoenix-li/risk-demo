package com.demo.risk.fieldcomplete.model;

import javax.validation.constraints.NotBlank;

public class ScriptInfo {

    @NotBlank(message = "scriptId 不能为空")
    private String scriptId;

    private String name;

    @NotBlank(message = "content 不能为空")
    private String content;

    public String getScriptId() {
        return scriptId;
    }

    public void setScriptId(String scriptId) {
        this.scriptId = scriptId;
    }

    public String getName() {
        return name != null ? name : scriptId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
