package com.demo.risk.ruleflow.model;

import com.demo.risk.ruleflow.ExpressionBuilder;

import javax.validation.constraints.NotBlank;

public class RuleInfo {

    @NotBlank(message = "ruleId 不能为空")
    private String ruleId;

    private String name;

    /** EXPR：表达式模式（condition 生效）；STRUCT：结构化模式（变量+操作符+阈值） */
    private String ruleType = "EXPR";

    /** Aviator 布尔表达式，ruleType=EXPR 时生效 */
    private String condition;

    /** ---- ruleType=STRUCT 时生效 ---- */

    /** 左值变量编码 */
    private String leftVar;

    /** GT/GE/LT/LE/EQ/NE/CONTAINS */
    private String operator;

    /** VALUE：固定值（thresholdValue）；VAR：引用变量（thresholdVar） */
    private String thresholdType = "VALUE";

    private String thresholdValue;

    private String thresholdVar;

    /** 命中加多少分 */
    private int hitScore;

    /** 命中动作：PASS / REVIEW / REJECT */
    @NotBlank(message = "hitAction 不能为空")
    private String hitAction;

    private boolean enabled = true;

    /** 对外展示用的完整表达式（只读，由后端编译） */
    public String getExpression() {
        if (ruleType == null || "EXPR".equals(ruleType)) {
            return condition;
        }
        return ExpressionBuilder.build(this);
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getName() {
        return name != null ? name : ruleId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public String getLeftVar() {
        return leftVar;
    }

    public void setLeftVar(String leftVar) {
        this.leftVar = leftVar;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getThresholdType() {
        return thresholdType;
    }

    public void setThresholdType(String thresholdType) {
        this.thresholdType = thresholdType;
    }

    public String getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(String thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public String getThresholdVar() {
        return thresholdVar;
    }

    public void setThresholdVar(String thresholdVar) {
        this.thresholdVar = thresholdVar;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public int getHitScore() {
        return hitScore;
    }

    public void setHitScore(int hitScore) {
        this.hitScore = hitScore;
    }

    public String getHitAction() {
        return hitAction;
    }

    public void setHitAction(String hitAction) {
        this.hitAction = hitAction;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
