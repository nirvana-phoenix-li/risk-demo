package com.demo.risk.controller;

import com.demo.risk.common.Result;
import com.demo.risk.model.RiskEvaluateRequest;
import com.demo.risk.model.RiskEvaluateResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/risk")
public class RiskController {

    /** 黑名单名单（演示用，内存存储） */
    private final Map<String, String> blacklist = new ConcurrentHashMap<>();

    public RiskController() {
        blacklist.put("user-100", "高频交易");
        blacklist.put("user-200", "关联欺诈账户");
    }

    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.ok("pong");
    }

    @PostMapping("/evaluate")
    public Result<RiskEvaluateResponse> evaluate(@Validated @RequestBody RiskEvaluateRequest request) {
        RiskEvaluateResponse response = doEvaluate(request.getUserId(), request.getAmount(), request.getScene());
        return Result.ok(response);
    }

    @GetMapping("/user/{userId}")
    public Result<RiskEvaluateResponse> queryUser(@PathVariable String userId) {
        RiskEvaluateResponse response = doEvaluate(userId, null, null);
        return Result.ok(response);
    }

    @GetMapping("/blacklist")
    public Result<List<String>> blacklist() {
        return Result.ok(Arrays.asList(blacklist.keySet().toArray(new String[0])));
    }

    @PostMapping("/blacklist/{userId}")
    public Result<String> addToBlacklist(@PathVariable String userId) {
        blacklist.put(userId, "手动加入");
        return Result.ok("已加入黑名单: " + userId);
    }

    @DeleteMapping("/blacklist/{userId}")
    public Result<String> removeFromBlacklist(@PathVariable String userId) {
        blacklist.remove(userId);
        return Result.ok("已移出黑名单: " + userId);
    }

    private RiskEvaluateResponse doEvaluate(String userId, BigDecimal amount, String scene) {
        if (blacklist.containsKey(userId)) {
            return new RiskEvaluateResponse(userId, 100, "REJECT", "黑名单: " + blacklist.get(userId));
        }

        int score = 10;
        String rule = "正常用户";

        if (amount != null && amount.compareTo(new BigDecimal("10000")) > 0) {
            score += 50;
            rule = "单笔大额交易";
        }
        if ("WITHDRAW".equals(scene)) {
            score += 30;
            rule = (score > 50 ? rule + " + " : "") + "提现场景";
        }
        if (userId != null && userId.endsWith("-risky")) {
            score += 40;
            rule = (score > 50 ? rule + " + " : "") + "命中风险特征";
        }

        String decision = score >= 80 ? "REJECT" : (score >= 40 ? "REVIEW" : "PASS");
        return new RiskEvaluateResponse(userId, Math.min(score, 99), decision, rule);
    }
}
