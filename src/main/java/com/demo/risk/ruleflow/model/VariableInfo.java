package com.demo.risk.ruleflow.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public class VariableInfo {

    /** 变量编码，同时作为表达式中的标识符，仅允许字母数字下划线 */
    @NotBlank(message = "varId 不能为空")
    @Pattern(regexp = "^[a-zA-Z_][a-zA-Z0-9_]*$", message = "varId 只能包含字母/数字/下划线，且以字母或下划线开头")
    private String varId;

    private String name;

    /** INPUT：从入参按路径取值；EXPR：Aviator 表达式计算 */
    @NotBlank(message = "varType 不能为空")
    private String varType;

    /** varType=INPUT 时为入参属性路径（支持 a.b.c）；varType=EXPR 时为 Aviator 表达式 */
    @NotBlank(message = "source 不能为空")
    private String source;

    /** NUMBER / STRING / BOOLEAN */
    private String valueType = "NUMBER";

    private String description;

    public String getVarId() {
        return varId;
    }

    public void setVarId(String varId) {
        this.varId = varId;
    }

    public String getName() {
        return name != null ? name : varId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVarType() {
        return varType;
    }

    public void setVarType(String varType) {
        this.varType = varType;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
