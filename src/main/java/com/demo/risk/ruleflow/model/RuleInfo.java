package com.demo.risk.ruleflow.model;

import javax.validation.constraints.NotBlank;

public class RuleInfo {

    @NotBlank(message = "ruleId 不能为空")
    private String ruleId;

    private String name;

    /** Aviator 布尔表达式，如 amount > 10000 && scene == 'WITHDRAW' */
    @NotBlank(message = "condition 不能为空")
    private String condition;

    /** 命中加多少分 */
    private int hitScore;

    /** 命中动作：PASS / REVIEW / REJECT */
    @NotBlank(message = "hitAction 不能为空")
    private String hitAction;

    private boolean enabled = true;

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
