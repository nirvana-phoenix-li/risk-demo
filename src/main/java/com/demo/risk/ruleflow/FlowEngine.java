package com.demo.risk.ruleflow;

import com.demo.risk.fieldcomplete.GroovyScriptEngine;
import com.demo.risk.fieldcomplete.ScriptStore;
import com.demo.risk.ruleflow.model.FlowInfo;
import com.demo.risk.ruleflow.model.FlowNode;
import com.demo.risk.ruleflow.model.FlowRunResult;
import com.demo.risk.ruleflow.model.RuleInfo;
import com.demo.risk.ruleflow.model.TraceItem;
import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Options;
import com.googlecode.aviator.exception.ExpressionRuntimeException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class FlowEngine {

    private static final Map<String, Integer> SEVERITY = new HashMap<>();

    static {
        SEVERITY.put("PASS", 1);
        SEVERITY.put("REVIEW", 2);
        SEVERITY.put("REJECT", 3);
    }

    private final RuleStore ruleStore;
    private final ScriptStore scriptStore;
    private final GroovyScriptEngine scriptEngine;

    public FlowEngine(RuleStore ruleStore, ScriptStore scriptStore, GroovyScriptEngine scriptEngine) {
        this.ruleStore = ruleStore;
        this.scriptStore = scriptStore;
        this.scriptEngine = scriptEngine;
        AviatorEvaluator.getInstance().setOption(Options.NIL_WHEN_PROPERTY_NOT_FOUND, true);
    }

    public FlowRunResult run(FlowInfo flow, Map<String, Object> input) {
        List<TraceItem> trace = new ArrayList<>();
        int totalScore = 0;
        String decision = "PASS";

        for (FlowNode node : flow.getNodes()) {
            long start = System.nanoTime();
            TraceItem item;
            if ("ENRICH".equals(node.getType())) {
                item = runEnrichNode(node, input);
            } else {
                EvalOutcome outcome = runRuleNode(node, input);
                totalScore += outcome.scoreDelta;
                if (SEVERITY.getOrDefault(outcome.decision, 1) > SEVERITY.getOrDefault(decision, 1)) {
                    decision = outcome.decision;
                }
                item = outcome.traceItem;
            }
            item = withElapsed(item, start);
            trace.add(item);
            if ("RULE".equals(item.getType()) && Boolean.TRUE.equals(item.getHit())
                    && "BREAK".equals(node.getOnHit())) {
                item.setDetail(item.getDetail() + " [短路]");
                break;
            }
        }
        return new FlowRunResult(flow.getFlowId(), flow.getName(), decision, totalScore, trace);
    }

    private TraceItem runEnrichNode(FlowNode node, Map<String, Object> input) {
        com.demo.risk.fieldcomplete.model.ScriptInfo script = scriptStore.require(node.getRefId());
        java.util.Set<String> before = new java.util.HashSet<>(input.keySet());
        scriptEngine.execute("flow@" + node.getNodeId() + "@" + script.getContent().hashCode(),
                script.getContent(), input);
        List<String> added = new ArrayList<>();
        for (String k : input.keySet()) {
            if (!before.contains(k)) {
                added.add(k);
            }
        }
        return new TraceItem(node.getNodeId(), script.getName(), "ENRICH", node.getRefId(),
                null, 0, 0, "补全字段: " + added);
    }

    private EvalOutcome runRuleNode(FlowNode node, Map<String, Object> input) {
        RuleInfo rule = ruleStore.require(node.getRefId());
        if (!rule.isEnabled()) {
            return new EvalOutcome(new TraceItem(node.getNodeId(), rule.getName(), "RULE",
                    node.getRefId(), null, 0, 0, "规则已停用，跳过"));
        }
        boolean hit;
        try {
            Object result = AviatorEvaluator.execute(rule.getCondition(), input, true);
            hit = Boolean.TRUE.equals(result);
        } catch (ExpressionRuntimeException e) {
            throw new RuleFlowException("规则[" + rule.getName() + "]条件执行失败: " + e.getMessage());
        }
        if (!hit) {
            return new EvalOutcome(new TraceItem(node.getNodeId(), rule.getName(), "RULE",
                    node.getRefId(), false, 0, 0, "未命中"));
        }
        return new EvalOutcome(new TraceItem(node.getNodeId(), rule.getName(), "RULE",
                node.getRefId(), true, rule.getHitScore(), 0,
                "命中 +" + rule.getHitScore() + " 分 → " + rule.getHitAction()), rule.getHitAction(), rule.getHitScore());
    }

    private TraceItem withElapsed(TraceItem item, long startNanos) {
        long ms = (System.nanoTime() - startNanos) / 1_000_000;
        return new TraceItem(item.getNodeId(), item.getNodeName(), item.getType(), item.getRefId(),
                item.getHit(), item.getScoreDelta(), ms, item.getDetail());
    }

    private static class EvalOutcome {
        final TraceItem traceItem;
        final String decision;
        final int scoreDelta;

        EvalOutcome(TraceItem traceItem) {
            this(traceItem, "PASS", 0);
        }

        EvalOutcome(TraceItem traceItem, String decision, int scoreDelta) {
            this.traceItem = traceItem;
            this.decision = decision;
            this.scoreDelta = scoreDelta;
        }
    }
}
