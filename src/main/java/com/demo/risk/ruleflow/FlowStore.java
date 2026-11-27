package com.demo.risk.ruleflow;

import com.demo.risk.ruleflow.model.FlowInfo;
import com.demo.risk.ruleflow.model.FlowNode;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FlowStore {

    private final Map<String, FlowInfo> flows = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        flows.put("flow-demo", demoFlow());
    }

    private FlowInfo demoFlow() {
        FlowInfo flow = new FlowInfo();
        flow.setFlowId("flow-demo");
        flow.setName("交易风控流程(演示)");
        flow.setNodes(Arrays.asList(
                node("n1-enrich", "ENRICH", "demo-basic", null),
                node("n2-channel", "RULE", "rule-risk-channel", "BREAK"),
                node("n3-amount", "RULE", "rule-high-amount", "CONTINUE"),
                node("n4-withdraw", "RULE", "rule-withdraw", "CONTINUE")
        ));
        return flow;
    }

    private FlowNode node(String nodeId, String type, String refId, String onHit) {
        FlowNode n = new FlowNode();
        n.setNodeId(nodeId);
        n.setType(type);
        n.setRefId(refId);
        if (onHit != null) {
            n.setOnHit(onHit);
        }
        return n;
    }

    public void save(FlowInfo flow) {
        if (flow.getNodes() == null) {
            flow.setNodes(new ArrayList<>());
        }
        for (FlowNode n : flow.getNodes()) {
            if (!"RULE".equals(n.getType()) && !"ENRICH".equals(n.getType())) {
                throw new RuleFlowException("节点类型必须是 RULE/ENRICH: " + n.getType());
            }
            if (n.getOnHit() == null) {
                n.setOnHit("CONTINUE");
            } else if (!"CONTINUE".equals(n.getOnHit()) && !"BREAK".equals(n.getOnHit())) {
                throw new RuleFlowException("onHit 必须是 CONTINUE/BREAK: " + n.getOnHit());
            }
        }
        flows.put(flow.getFlowId(), flow);
    }

    public FlowInfo get(String flowId) {
        return flows.get(flowId);
    }

    public FlowInfo require(String flowId) {
        FlowInfo flow = flows.get(flowId);
        if (flow == null) {
            throw new RuleFlowException("流程不存在: " + flowId);
        }
        return flow;
    }

    public FlowInfo delete(String flowId) {
        return flows.remove(flowId);
    }

    public java.util.List<FlowInfo> list() {
        return new ArrayList<>(flows.values());
    }
}
