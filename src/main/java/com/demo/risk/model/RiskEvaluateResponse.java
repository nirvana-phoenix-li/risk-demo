package com.demo.risk.model;

public class RiskEvaluateResponse {

    private String userId;
    /** 0-100 风险分 */
    private int riskScore;
    /** PASS / REVIEW / REJECT */
    private String decision;
    /** 命中的规则 */
    private String hitRule;

    public RiskEvaluateResponse(String userId, int riskScore, String decision, String hitRule) {
        this.userId = userId;
        this.riskScore = riskScore;
        this.decision = decision;
        this.hitRule = hitRule;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getHitRule() {
        return hitRule;
    }

    public void setHitRule(String hitRule) {
        this.hitRule = hitRule;
    }
}
