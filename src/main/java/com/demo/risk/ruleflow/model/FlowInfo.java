package com.demo.risk.ruleflow.model;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;

public class FlowInfo {

    @NotBlank(message = "flowId 不能为空")
    private String flowId;

    private String name;

    /** 允许为空：支持先建流程骨架再编排节点 */
    @Valid
    private List<FlowNode> nodes;

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public String getName() {
        return name != null ? name : flowId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<FlowNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<FlowNode> nodes) {
        this.nodes = nodes;
    }
}
