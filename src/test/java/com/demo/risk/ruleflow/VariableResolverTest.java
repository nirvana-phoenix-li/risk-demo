package com.demo.risk.ruleflow;

import com.demo.risk.ruleflow.model.VariableInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VariableResolverTest {

    private VarStore varStore;
    private VariableResolver resolver;

    @BeforeEach
    void setUp() {
        varStore = new VarStore();
        varStore.init();
        resolver = new VariableResolver(varStore);
    }

    @Test
    void resolvesInputVariable() {
        Map<String, Object> env = resolver.resolve(input("amount", 15000));
        assertThat(env.get("amount")).isEqualTo(15000);
    }

    @Test
    void resolvesNestedPath() {
        Map<String, Object> input = input("thresholds", java.util.Collections.singletonMap("bigAmount", 20000));
        Map<String, Object> env = resolver.resolve(input);

        assertThat(env.get("bigAmountThreshold")).isEqualTo(20000);
    }

    @Test
    void missingPathResolvesToNull() {
        Map<String, Object> env = resolver.resolve(input("userId", "u001"));
        assertThat(env.get("bigAmountThreshold")).isNull();
    }

    @Test
    void exprVariableComputes() {
        Map<String, Object> env = resolver.resolve(input("amount", 15000));
        assertThat(env.get("amountDouble")).isEqualTo(30000L);
    }

    @Test
    void brokenExprVariableResolvesToNull() {
        VariableInfo bad = new VariableInfo();
        bad.setVarId("badVar");
        bad.setName("坏变量");
        bad.setVarType("EXPR");
        bad.setSource("missingVar * 2");
        varStore.save(bad);

        Map<String, Object> env = resolver.resolve(input("amount", 1));
        assertThat(env.get("badVar")).isNull();
    }

    @Test
    void keepsOriginalInputFields() {
        Map<String, Object> env = resolver.resolve(input("userId", "u001", "amount", 100));
        assertThat(env.get("userId")).isEqualTo("u001");
        assertThat(env.get("amount")).isEqualTo(100);
    }

    @Test
    void varStoreValidation() {
        VariableInfo badType = new VariableInfo();
        badType.setVarId("v1");
        badType.setVarType("CALC");
        badType.setSource("amount");

        assertThatThrownBy(() -> varStore.save(badType))
                .isInstanceOf(RuleFlowException.class)
                .hasMessageContaining("varType");

        VariableInfo badId = new VariableInfo();
        badId.setVarId("2bad-id");
        badId.setVarType("INPUT");
        badId.setSource("amount");

        assertThatThrownBy(() -> varStore.save(badId))
                .isInstanceOf(RuleFlowException.class)
                .hasMessageContaining("varId");

        VariableInfo badExpr = new VariableInfo();
        badExpr.setVarId("v2");
        badExpr.setVarType("EXPR");
        badExpr.setName("语法错误变量");
        badExpr.setSource("amount >");

        assertThatThrownBy(() -> varStore.save(badExpr))
                .isInstanceOf(RuleFlowException.class)
                .hasMessageContaining("语法错误");
    }

    private Map<String, Object> input(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }
}
