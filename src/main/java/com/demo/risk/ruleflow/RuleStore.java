package com.demo.risk.ruleflow;

import com.demo.risk.ruleflow.model.RuleInfo;
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

    @PostConstruct
    public void init() {
        save(rule("rule-risk-channel", "高危渠道", "channel == 'RISK_CHANNEL'", 40, "REJECT"));
        save(rule("rule-high-amount", "大额交易", "amount > 10000", 50, "REVIEW"));
        save(rule("rule-withdraw", "提现场景", "scene == 'WITHDRAW'", 30, "REVIEW"));
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
        rules.put(info.getRuleId(), info);
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
