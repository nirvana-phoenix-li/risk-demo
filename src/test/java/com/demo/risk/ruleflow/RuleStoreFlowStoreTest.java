package com.demo.risk.ruleflow;

import com.demo.risk.ruleflow.model.FlowInfo;
import com.demo.risk.ruleflow.model.FlowNode;
import com.demo.risk.ruleflow.model.RuleInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuleStoreFlowStoreTest {

    private RuleStore ruleStore;
    private FlowStore flowStore;

    @BeforeEach
    void setUp() {
        ruleStore = new RuleStore();
        ruleStore.init();
        flowStore = new FlowStore();
        flowStore.init();
    }

    @Test
    void preloadsDemoRules() {
        assertThat(ruleStore.get("rule-high-amount")).isNotNull();
        assertThat(ruleStore.get("rule-risk-channel").getHitAction()).isEqualTo("REJECT");
    }

    @Test
    void rejectsInvalidHitAction() {
        RuleInfo bad = rule("r1", "amount > 1");
        bad.setHitAction("BAN");

        assertThatThrownBy(() -> ruleStore.save(bad))
                .isInstanceOf(RuleFlowException.class)
                .hasMessageContaining("hitAction");
    }

    @Test
    void requireThrowsForMissingRule() {
        assertThatThrownBy(() -> ruleStore.require("ghost"))
                .isInstanceOf(RuleFlowException.class)
                .hasMessageContaining("规则不存在");
    }

    @Test
    void flowSaveDefaultsOnHitToContinue() {
        FlowNode n = new FlowNode();
        n.setNodeId("n1");
        n.setType("RULE");
        n.setRefId("rule-high-amount");

        FlowInfo flow = new FlowInfo();
        flow.setFlowId("f1");
        flow.setNodes(Collections.singletonList(n));

        flowStore.save(flow);

        assertThat(flowStore.get("f1").getNodes().get(0).getOnHit()).isEqualTo("CONTINUE");
    }

    @Test
    void flowSaveRejectsInvalidNodeType() {
        FlowNode n = new FlowNode();
        n.setNodeId("n1");
        n.setType("SCRIPT");
        n.setRefId("whatever");

        FlowInfo flow = new FlowInfo();
        flow.setFlowId("f2");
        flow.setNodes(Collections.singletonList(n));

        assertThatThrownBy(() -> flowStore.save(flow))
                .isInstanceOf(RuleFlowException.class)
                .hasMessageContaining("节点类型");
    }

    @Test
    void flowSaveRejectsInvalidOnHit() {
        FlowNode n = new FlowNode();
        n.setNodeId("n1");
        n.setType("RULE");
        n.setRefId("rule-high-amount");
        n.setOnHit("STOP");

        FlowInfo flow = new FlowInfo();
        flow.setFlowId("f3");
        flow.setNodes(Collections.singletonList(n));

        assertThatThrownBy(() -> flowStore.save(flow))
                .isInstanceOf(RuleFlowException.class)
                .hasMessageContaining("onHit");
    }

    @Test
    void preloadsDemoFlow() {
        assertThat(flowStore.get("flow-demo")).isNotNull();
        assertThat(flowStore.require("flow-demo").getNodes()).hasSize(4);
    }

    @Test
    void deleteWorks() {
        assertThat(flowStore.delete("flow-demo")).isNotNull();
        assertThatThrownBy(() -> flowStore.require("flow-demo"))
                .isInstanceOf(RuleFlowException.class);
    }

    private RuleInfo rule(String id, String condition) {
        RuleInfo info = new RuleInfo();
        info.setRuleId(id);
        info.setCondition(condition);
        info.setHitScore(10);
        info.setHitAction("REVIEW");
        return info;
    }
}
