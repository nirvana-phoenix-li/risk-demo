package com.demo.risk.ruleflow;

import com.demo.risk.fieldcomplete.GroovyScriptEngine;
import com.demo.risk.fieldcomplete.ScriptStore;
import com.demo.risk.ruleflow.model.FlowInfo;
import com.demo.risk.ruleflow.model.FlowNode;
import com.demo.risk.ruleflow.model.FlowRunResult;
import com.demo.risk.ruleflow.model.RuleInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FlowEngineTest {

    private RuleStore ruleStore;
    private FlowEngine engine;

    @BeforeEach
    void setUp() {
        VarStore varStore = new VarStore();
        varStore.init();
        ruleStore = new RuleStore(varStore);
        ruleStore.init();
        ScriptStore scriptStore = new ScriptStore();
        scriptStore.init();
        engine = new FlowEngine(ruleStore, scriptStore, new GroovyScriptEngine(),
                new VariableResolver(varStore));
    }

    @Test
    void demoFlowReviewsBigAmount() {
        FlowRunResult r = engine.run(demoFlow(), input(
                "userId", "u001", "amount", 15000, "scene", "LOGIN", "channel", "NORMAL"));

        assertThat(r.getDecision()).isEqualTo("REVIEW");
        assertThat(r.getTotalScore()).isEqualTo(50);
        assertThat(r.getTrace()).hasSize(4);
        assertThat(r.getTrace().get(0).getType()).isEqualTo("ENRICH");
    }

    @Test
    void riskChannelShortCircuits() {
        FlowRunResult r = engine.run(demoFlow(), input(
                "userId", "u001", "amount", 15000, "scene", "LOGIN", "channel", "RISK_CHANNEL"));

        assertThat(r.getDecision()).isEqualTo("REJECT");
        assertThat(r.getTotalScore()).isEqualTo(40);
        assertThat(r.getTrace()).hasSize(2);
        assertThat(r.getTrace().get(0).getType()).isEqualTo("ENRICH");
        assertThat(r.getTrace().get(1).getDetail()).contains("短路");
    }

    @Test
    void missingVariableDoesNotHit() {
        FlowRunResult r = engine.run(flow(node("n1", "rule-high-amount", "BREAK")),
                input("userId", "u001"));

        assertThat(r.getDecision()).isEqualTo("PASS");
        assertThat(r.getTotalScore()).isEqualTo(0);
        assertThat(r.getTrace().get(0).getHit()).isFalse();
    }

    @Test
    void enrichResultFeedsLaterRule() {
        FlowNode enrich = node("n1", "ENRICH", "demo-basic", null);
        RuleInfo derived = rule("rule-derived-high", "highAmount == true", 60, "REJECT");
        ruleStore.save(derived);
        FlowRunResult r = engine.run(flow(enrich, node("n2", "rule-derived-high", "CONTINUE")),
                input("amount", 15000));

        assertThat(r.getDecision()).isEqualTo("REJECT");
        assertThat(r.getTotalScore()).isEqualTo(60);
        assertThat(r.getTrace().get(0).getDetail()).contains("highAmount");
    }

    @Test
    void disabledRuleIsSkipped() {
        ruleStore.get("rule-high-amount").setEnabled(false);
        FlowRunResult r = engine.run(flow(node("n1", "rule-high-amount", "BREAK")),
                input("amount", 20000));

        assertThat(r.getDecision()).isEqualTo("PASS");
        assertThat(r.getTrace().get(0).getDetail()).contains("已停用");
        assertThat(r.getTrace().get(0).getHit()).isNull();
    }

    @Test
    void rejectWithContinueStillRunsLaterNodes() {
        RuleInfo second = rule("rule-second", "amount > 500", 10, "REJECT");
        ruleStore.save(second);
        FlowRunResult r = engine.run(
                flow(node("n1", "rule-high-amount", "CONTINUE"), node("n2", "rule-second", "CONTINUE")),
                input("amount", 20000));

        assertThat(r.getDecision()).isEqualTo("REJECT");
        assertThat(r.getTotalScore()).isEqualTo(60);
        assertThat(r.getTrace()).hasSize(2);
    }

    @Test
    void sceneStringComparisonWorks() {
        FlowRunResult r = engine.run(flow(node("n1", "rule-withdraw", "CONTINUE")),
                input("scene", "WITHDRAW"));

        assertThat(r.getTrace().get(0).getHit()).isTrue();
        assertThat(r.getDecision()).isEqualTo("REVIEW");
    }

    @Test
    void structRuleHitsWhenAmountOverSceneThreshold() {
        FlowRunResult r = engine.run(flow(node("n1", "rule-struct-big", "CONTINUE")),
                input("amount", 15000, "thresholds", nested("bigAmount", 10000)));

        assertThat(r.getTrace().get(0).getHit()).isTrue();
        assertThat(r.getDecision()).isEqualTo("REVIEW");
        assertThat(r.getTotalScore()).isEqualTo(50);
    }

    @Test
    void structRuleSkipsWhenSceneThresholdIsHigher() {
        FlowRunResult r = engine.run(flow(node("n1", "rule-struct-big", "CONTINUE")),
                input("amount", 15000, "thresholds", nested("bigAmount", 20000)));

        assertThat(r.getTrace().get(0).getHit()).isFalse();
        assertThat(r.getDecision()).isEqualTo("PASS");
    }

    @Test
    void structRuleWithoutThresholdFieldDoesNotHit() {
        FlowRunResult r = engine.run(flow(node("n1", "rule-struct-big", "CONTINUE")),
                input("amount", 15000));

        assertThat(r.getTrace().get(0).getHit()).isFalse();
    }

    private FlowInfo demoFlow() {
        FlowInfo f = new FlowInfo();
        f.setFlowId("flow-x");
        f.setName("测试流程");
        f.setNodes(Arrays.asList(
                node("n0", "ENRICH", "demo-basic", null),
                node("n1", "rule-risk-channel", "BREAK"),
                node("n2", "rule-high-amount", "CONTINUE"),
                node("n3", "rule-withdraw", "CONTINUE")));
        return f;
    }

    private FlowInfo flow(FlowNode... nodes) {
        FlowInfo f = new FlowInfo();
        f.setFlowId("flow-x");
        f.setName("测试流程");
        f.setNodes(Arrays.asList(nodes));
        return f;
    }

    private FlowNode node(String nodeId, String refId, String onHit) {
        FlowNode n = new FlowNode();
        n.setNodeId(nodeId);
        n.setType("RULE");
        n.setRefId(refId);
        n.setOnHit(onHit);
        return n;
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

    private RuleInfo rule(String id, String condition, int score, String action) {
        RuleInfo info = new RuleInfo();
        info.setRuleId(id);
        info.setCondition(condition);
        info.setHitScore(score);
        info.setHitAction(action);
        return info;
    }

    private Map<String, Object> input(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    private Map<String, Object> nested(String k, Object v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(k, v);
        return m;
    }
}
