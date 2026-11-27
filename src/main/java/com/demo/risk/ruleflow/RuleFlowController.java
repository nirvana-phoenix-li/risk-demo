package com.demo.risk.ruleflow;

import com.demo.risk.common.Result;
import com.demo.risk.ruleflow.model.FlowInfo;
import com.demo.risk.ruleflow.model.FlowRunResult;
import com.demo.risk.ruleflow.model.RuleInfo;
import com.googlecode.aviator.AviatorEvaluator;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rule-engine")
public class RuleFlowController {

    private final RuleStore ruleStore;
    private final FlowStore flowStore;
    private final FlowEngine flowEngine;

    public RuleFlowController(RuleStore ruleStore, FlowStore flowStore, FlowEngine flowEngine) {
        this.ruleStore = ruleStore;
        this.flowStore = flowStore;
        this.flowEngine = flowEngine;
    }

    // ---------- 规则 ----------

    @PostMapping("/rules")
    public Result<RuleInfo> saveRule(@Validated @RequestBody RuleInfo info) {
        ruleStore.save(info);
        return Result.ok(info);
    }

    @GetMapping("/rules")
    public Result<List<RuleInfo>> listRules() {
        return Result.ok(ruleStore.list());
    }

    @GetMapping("/rules/{ruleId}")
    public Result<RuleInfo> getRule(@PathVariable String ruleId) {
        return Result.ok(ruleStore.require(ruleId));
    }

    @DeleteMapping("/rules/{ruleId}")
    public Result<String> deleteRule(@PathVariable String ruleId) {
        if (ruleStore.delete(ruleId) == null) {
            return Result.fail(404, "规则不存在: " + ruleId);
        }
        return Result.ok("已删除: " + ruleId);
    }

    @PostMapping("/rules/{ruleId}/try")
    public Result<Map<String, Object>> tryRule(@PathVariable String ruleId,
                                               @RequestBody Map<String, Object> input) {
        RuleInfo rule = ruleStore.require(ruleId);
        Map<String, Object> result = new LinkedHashMap<>();
        Object eval = AviatorEvaluator.execute(rule.getCondition(), input, true);
        boolean hit = Boolean.TRUE.equals(eval);
        result.put("hit", hit);
        result.put("hitScore", hit ? rule.getHitScore() : 0);
        result.put("hitAction", hit ? rule.getHitAction() : null);
        result.put("condition", rule.getCondition());
        result.put("input", input);
        return Result.ok(result);
    }

    // ---------- 流程 ----------

    @PostMapping("/flows")
    public Result<FlowInfo> saveFlow(@Validated @RequestBody FlowInfo flow) {
        flowStore.save(flow);
        return Result.ok(flow);
    }

    @GetMapping("/flows")
    public Result<List<FlowInfo>> listFlows() {
        return Result.ok(flowStore.list());
    }

    @GetMapping("/flows/{flowId}")
    public Result<FlowInfo> getFlow(@PathVariable String flowId) {
        return Result.ok(flowStore.require(flowId));
    }

    @DeleteMapping("/flows/{flowId}")
    public Result<String> deleteFlow(@PathVariable String flowId) {
        if (flowStore.delete(flowId) == null) {
            return Result.fail(404, "流程不存在: " + flowId);
        }
        return Result.ok("已删除: " + flowId);
    }

    @PostMapping("/flows/{flowId}/run")
    public Result<FlowRunResult> runFlow(@PathVariable String flowId,
                                         @RequestBody Map<String, Object> input) {
        FlowInfo flow = flowStore.require(flowId);
        Map<String, Object> working = new LinkedHashMap<>(input);
        return Result.ok(flowEngine.run(flow, working));
    }
}
