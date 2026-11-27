package com.demo.risk.ruleflow.model;

public class TraceItem {

    private String nodeId;
    private String nodeName;
    /** RULE / ENRICH */
    private String type;
    private String refId;
    /** RULE: 是否命中；ENRICH: null */
    private Boolean hit;
    private int scoreDelta;
    private long elapsedMs;
    private String detail;

    public TraceItem(String nodeId, String nodeName, String type, String refId,
                     Boolean hit, int scoreDelta, long elapsedMs, String detail) {
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.type = type;
        this.refId = refId;
        this.hit = hit;
        this.scoreDelta = scoreDelta;
        this.elapsedMs = elapsedMs;
        this.detail = detail;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getType() {
        return type;
    }

    public String getRefId() {
        return refId;
    }

    public Boolean getHit() {
        return hit;
    }

    public int getScoreDelta() {
        return scoreDelta;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
