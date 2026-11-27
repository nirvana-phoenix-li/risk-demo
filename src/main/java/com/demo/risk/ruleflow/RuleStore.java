package com.demo.risk.ruleflow;

import com.demo.risk.ruleflow.model.RuleInfo;
import com.demo.risk.ruleflow.model.VariableInfo;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RuleStore {

    private static final Set<String> VALID_ACTIONS =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList("PASS", "REVIEW", "REJECT")));

    private final Map<String, RuleInfo> rules = new ConcurrentHashMap<>();

    private final VarStore varStore;

    public RuleStore(VarStore varStore) {
        this.varStore = varStore;
    }

    @PostConstruct
    public void init() {
        save(rule("rule-risk-channel", "高危渠道", "channel == 'RISK_CHANNEL'", 40, "REJECT"));
        save(rule("rule-high-amount", "大额交易", "amount > 10000", 50, "REVIEW"));
        save(rule("rule-withdraw", "提现场景", "scene == 'WITHDRAW'", 30, "REVIEW"));

        RuleInfo struct = rule("rule-struct-big", "金额超场景阈值", null, 50, "REVIEW");
        struct.setRuleType("STRUCT");
        struct.setLeftVar("amount");
        struct.setOperator("GT");
        struct.setThresholdType("VAR");
        struct.setThresholdVar("bigAmountThreshold");
        save(struct);
    }

    private RuleInfo rule(String id, String name, String condition, int score, String action) {
        RuleInfo info = new RuleInfo();
        info.setRuleId(id);
        info.setName(name);
        info.setCondition(condition);
        info.setHitScore(score);
        info.setHitAction(action);
        return info;
    }

    public void save(RuleInfo info) {
        if (!VALID_ACTIONS.contains(info.getHitAction())) {
            throw new RuleFlowException("hitAction 必须是 PASS/REVIEW/REJECT: " + info.getHitAction());
        }
        if (info.getRuleType() == null || "EXPR".equals(info.getRuleType())) {
            if (info.getCondition() == null || info.getCondition().trim().isEmpty()) {
                throw new RuleFlowException("表达式模式的规则 condition 不能为空");
            }
            info.setRuleType("EXPR");
        } else if ("STRUCT".equals(info.getRuleType())) {
            validateStruct(info);
        } else {
            throw new RuleFlowException("ruleType 必须是 EXPR/STRUCT: " + info.getRuleType());
        }
        rules.put(info.getRuleId(), info);
    }

    private void validateStruct(RuleInfo info) {
        ExpressionBuilder.requireIdentifier(info.getLeftVar(), "leftVar");
        if (!ExpressionBuilder.OPERATORS.contains(info.getOperator())) {
            throw new RuleFlowException("不支持的操作符: " + info.getOperator());
        }
        boolean varThreshold = "VAR".equals(info.getThresholdType());
        if (!varThreshold && !"VALUE".equals(info.getThresholdType())) {
            throw new RuleFlowException("thresholdType 必须是 VALUE/VAR: " + info.getThresholdType());
        }
        if (varThreshold) {
            VariableInfo thresholdVarInfo = varStore.require(info.getThresholdVar());
            if (ExpressionBuilder.isNumericOp(info.getOperator())
                    && !"NUMBER".equals(thresholdVarInfo.getValueType())) {
                throw new RuleFlowException("操作符 " + info.getOperator() + " 要求阈值为数值型变量: "
                        + info.getThresholdVar());
            }
        } else if ("CONTAINS".equals(info.getOperator())) {
            if (info.getThresholdValue() == null || info.getThresholdValue().isEmpty()) {
                throw new RuleFlowException("CONTAINS 的阈值不能为空");
            }
        }

        VariableInfo leftInfo = varStore.get(info.getLeftVar());
        if (leftInfo != null && ExpressionBuilder.isNumericOp(info.getOperator())
                && !"NUMBER".equals(leftInfo.getValueType())) {
            throw new RuleFlowException("操作符 " + info.getOperator() + " 不适用于非数值变量: " + info.getLeftVar());
        }

        ExpressionBuilder.build(info);
    }

    public RuleInfo get(String ruleId) {
        return rules.get(ruleId);
    }

    public RuleInfo require(String ruleId) {
        RuleInfo info = rules.get(ruleId);
        if (info == null) {
            throw new RuleFlowException("规则不存在: " + ruleId);
        }
        return info;
    }

    public RuleInfo delete(String ruleId) {
        return rules.remove(ruleId);
    }

    public java.util.List<RuleInfo> list() {
        return new ArrayList<>(rules.values());
    }
}
