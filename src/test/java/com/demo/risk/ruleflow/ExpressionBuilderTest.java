package com.demo.risk.ruleflow;

import com.demo.risk.ruleflow.model.RuleInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpressionBuilderTest {

    private RuleInfo struct(String left, String op, String thresholdType, String thresholdValue, String thresholdVar) {
        RuleInfo r = new RuleInfo();
        r.setRuleType("STRUCT");
        r.setLeftVar(left);
        r.setOperator(op);
        r.setThresholdType(thresholdType);
        r.setThresholdValue(thresholdValue);
        r.setThresholdVar(thresholdVar);
        return r;
    }

    @Test
    void numericValueLiteral() {
        assertThat(ExpressionBuilder.build(struct("amount", "GT", "VALUE", "10000", null)))
                .isEqualTo("amount > 10000");
        assertThat(ExpressionBuilder.build(struct("amount", "LE", "VALUE", "99.5", null)))
                .isEqualTo("amount <= 99.5");
    }

    @Test
    void stringAndBooleanLiterals() {
        assertThat(ExpressionBuilder.build(struct("scene", "EQ", "VALUE", "WITHDRAW", null)))
                .isEqualTo("scene == 'WITHDRAW'");
        assertThat(ExpressionBuilder.build(struct("isVip", "EQ", "VALUE", "true", null)))
                .isEqualTo("isVip == true");
        assertThat(ExpressionBuilder.build(struct("channel", "NE", "VALUE", "RISK", null)))
                .isEqualTo("channel != 'RISK'");
    }

    @Test
    void varThresholdReference() {
        assertThat(ExpressionBuilder.build(struct("amount", "GT", "VAR", null, "bigAmountThreshold")))
                .isEqualTo("(bigAmountThreshold != nil) && (amount > bigAmountThreshold)");
    }

    @Test
    void containsOperator() {
        assertThat(ExpressionBuilder.build(struct("remark", "CONTAINS", "VALUE", "黑名单", null)))
                .isEqualTo("contains(remark, '黑名单')");
    }

    @Test
    void quotesStrippedFromStringLiteral() {
        assertThat(ExpressionBuilder.build(struct("scene", "EQ", "VALUE", "With'Draw", null)))
                .isEqualTo("scene == 'WithDraw'");
    }

    @Test
    void invalidOperatorRejected() {
        assertThatThrownBy(() -> ExpressionBuilder.build(struct("amount", "BETWEEN", "VALUE", "1", null)))
                .isInstanceOf(RuleFlowException.class)
                .hasMessageContaining("操作符");
    }

    @Test
    void invalidIdentifierRejected() {
        assertThatThrownBy(() -> ExpressionBuilder.build(struct("a-b", "GT", "VALUE", "1", null)))
                .isInstanceOf(RuleFlowException.class)
                .hasMessageContaining("leftVar");
    }

    @Test
    void containsWithVarThresholdRejected() {
        assertThatThrownBy(() -> ExpressionBuilder.build(struct("remark", "CONTAINS", "VAR", null, "kw")))
                .isInstanceOf(RuleFlowException.class)
                .hasMessageContaining("CONTAINS");
    }

    @Test
    void blankThresholdRejected() {
        assertThatThrownBy(() -> ExpressionBuilder.build(struct("amount", "GT", "VALUE", "  ", null)))
                .isInstanceOf(RuleFlowException.class)
                .hasMessageContaining("阈值不能为空");
    }
}
