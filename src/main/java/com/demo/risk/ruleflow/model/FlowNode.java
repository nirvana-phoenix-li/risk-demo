package com.demo.risk.ruleflow.model;

import javax.validation.constraints.NotBlank;

public class FlowNode {

    @NotBlank(message = "nodeId 不能为空")
    private String nodeId;

    /** RULE（规则判断）/ ENRICH（字段补全） */
    @NotBlank(message = "type 不能为空")
    private String type;

    /** type=RULE 时指向 ruleId；type=ENRICH 时指向补全脚本 scriptId */
    @NotBlank(message = "refId 不能为空")
    private String refId;

    /** type=RULE 时生效：CONTINUE（命中继续）/ BREAK（命中短路） */
    private String onHit = "CONTINUE";

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRefId() {
        return refId;
    }

    public void setRefId(String refId) {
        this.refId = refId;
    }

    public String getOnHit() {
        return onHit;
    }

    public void setOnHit(String onHit) {
        this.onHit = onHit;
    }
}
