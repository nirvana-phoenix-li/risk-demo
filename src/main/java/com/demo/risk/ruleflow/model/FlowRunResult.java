package com.demo.risk.ruleflow.model;

import java.util.List;

public class FlowRunResult {

    private String flowId;
    private String flowName;
    /** 最终决策：PASS / REVIEW / REJECT */
    private String decision;
    private int totalScore;
    private List<TraceItem> trace;

    public FlowRunResult(String flowId, String flowName, String decision, int totalScore, List<TraceItem> trace) {
        this.flowId = flowId;
        this.flowName = flowName;
        this.decision = decision;
        this.totalScore = totalScore;
        this.trace = trace;
    }

    public String getFlowId() {
        return flowId;
    }

    public String getFlowName() {
        return flowName;
    }

    public String getDecision() {
        return decision;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public List<TraceItem> getTrace() {
        return trace;
    }
}
