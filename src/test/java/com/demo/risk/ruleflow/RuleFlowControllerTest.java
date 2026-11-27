package com.demo.risk.ruleflow;

import com.demo.risk.common.Result;
import com.demo.risk.fieldcomplete.GroovyScriptEngine;
import com.demo.risk.fieldcomplete.ScriptStore;
import com.demo.risk.ruleflow.model.FlowInfo;
import com.demo.risk.ruleflow.model.FlowRunResult;
import com.demo.risk.ruleflow.model.RuleInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuleFlowControllerTest {

    private RuleFlowController controller;

    @BeforeEach
    void setUp() {
        VarStore varStore = new VarStore();
        varStore.init();
        RuleStore ruleStore = new RuleStore(varStore);
        ruleStore.init();
        FlowStore flowStore = new FlowStore();
        flowStore.init();
        ScriptStore scriptStore = new ScriptStore();
        scriptStore.init();
        FlowEngine flowEngine = new FlowEngine(ruleStore, scriptStore, new GroovyScriptEngine(),
                new VariableResolver(varStore));
        controller = new RuleFlowController(ruleStore, flowStore, flowEngine, varStore,
                new VariableResolver(varStore));
    }

    @Test
    void tryRuleHit() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("amount", 20000);

        Result<Map<String, Object>> r = controller.tryRule("rule-high-amount", input);

        assertThat(r.getCode()).isEqualTo(0);
        assertThat(r.getData().get("hit")).isEqualTo(true);
        assertThat(r.getData().get("hitScore")).isEqualTo(50);
        assertThat(r.getData().get("hitAction")).isEqualTo("REVIEW");
    }

    @Test
    void tryRuleMiss() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("amount", 100);

        Result<Map<String, Object>> r = controller.tryRule("rule-high-amount", input);

        assertThat(r.getData().get("hit")).isEqualTo(false);
        assertThat(r.getData().get("hitScore")).isEqualTo(0);
    }

    @Test
    void tryRuleUnknownThrows() {
        assertThatThrownBy(() -> controller.tryRule("ghost", new LinkedHashMap<>()))
                .isInstanceOf(RuleFlowException.class)
                .hasMessageContaining("规则不存在");
    }

    @Test
    void saveRuleWithInvalidActionThrows() {
        RuleInfo bad = new RuleInfo();
        bad.setRuleId("r-bad");
        bad.setCondition("amount > 1");
        bad.setHitAction("BAN");

        assertThatThrownBy(() -> controller.saveRule(bad))
                .isInstanceOf(RuleFlowException.class)
                .hasMessageContaining("hitAction");
    }

    @Test
    void runDemoFlow() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("userId", "u001");
        input.put("amount", 15000);
        input.put("scene", "LOGIN");
        input.put("channel", "NORMAL");

        Result<FlowRunResult> r = controller.runFlow("flow-demo", input);

        assertThat(r.getCode()).isEqualTo(0);
        assertThat(r.getData().getDecision()).isEqualTo("REVIEW");
        assertThat(r.getData().getTotalScore()).isEqualTo(50);
    }

    @Test
    void runUnknownFlowThrows() {
        assertThatThrownBy(() -> controller.runFlow("ghost", new LinkedHashMap<>()))
                .isInstanceOf(RuleFlowException.class)
                .hasMessageContaining("流程不存在");
    }

    @Test
    void deleteMissingRuleReturns404() {
        assertThat(controller.deleteRule("ghost").getCode()).isEqualTo(404);
    }

    @Test
    void ruleCrudRoundTrip() {
        RuleInfo info = new RuleInfo();
        info.setRuleId("r-t1");
        info.setCondition("amount > 100");
        info.setHitScore(5);
        info.setHitAction("PASS");

        controller.saveRule(info);
        assertThat(controller.getRule("r-t1").getData().getCondition()).isEqualTo("amount > 100");
        assertThat(controller.listRules().getData())
                .extracting(RuleInfo::getRuleId)
                .contains("r-t1");

        controller.deleteRule("r-t1");
        assertThat(controller.listRules().getData())
                .extracting(RuleInfo::getRuleId)
                .doesNotContain("r-t1");
    }
}
