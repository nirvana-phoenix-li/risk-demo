package com.demo.risk;

import com.demo.risk.controller.RiskController;
import com.demo.risk.model.RiskEvaluateRequest;
import com.demo.risk.model.RiskEvaluateResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RiskControllerTest {

    private final RiskController controller = new RiskController();

    @Test
    void pingReturnsPong() {
        assertThat(controller.ping().getData()).isEqualTo("pong");
    }

    @Test
    void normalLoginPasses() {
        RiskEvaluateResponse r = evaluate("user-001", "1500", "LOGIN");
        assertThat(r.getDecision()).isEqualTo("PASS");
        assertThat(r.getRiskScore()).isEqualTo(10);
    }

    @Test
    void bigAmountWithdrawRejects() {
        RiskEvaluateResponse r = evaluate("user-001", "50000", "WITHDRAW");
        assertThat(r.getDecision()).isEqualTo("REJECT");
        assertThat(r.getRiskScore()).isEqualTo(90);
    }

    @Test
    void midAmountWithdrawNeedsReview() {
        RiskEvaluateResponse r = evaluate("user-001", "5000", "WITHDRAW");
        assertThat(r.getDecision()).isEqualTo("REVIEW");
        assertThat(r.getRiskScore()).isEqualTo(40);
    }

    @Test
    void riskySuffixUserNeedsReview() {
        RiskEvaluateResponse r = evaluate("user-001-risky", "100", "LOGIN");
        assertThat(r.getDecision()).isEqualTo("REVIEW");
        assertThat(r.getRiskScore()).isEqualTo(50);
    }

    @Test
    void blacklistUserRejects() {
        RiskEvaluateResponse r = controller.queryUser("user-100").getData();
        assertThat(r.getDecision()).isEqualTo("REJECT");
        assertThat(r.getRiskScore()).isEqualTo(100);
        assertThat(r.getHitRule()).contains("高频交易");
    }

    @Test
    void blacklistAddAndRemoveAffectsEvaluation() {
        controller.addToBlacklist("user-x");
        assertThat(controller.queryUser("user-x").getData().getDecision()).isEqualTo("REJECT");

        controller.removeFromBlacklist("user-x");
        assertThat(controller.queryUser("user-x").getData().getDecision()).isEqualTo("PASS");
    }

    @Test
    void blacklistListContainsPreloadedUsers() {
        assertThat(controller.blacklist().getData()).contains("user-100", "user-200");
    }

    private RiskEvaluateResponse evaluate(String userId, String amount, String scene) {
        RiskEvaluateRequest req = new RiskEvaluateRequest();
        req.setUserId(userId);
        req.setAmount(new BigDecimal(amount));
        req.setScene(scene);
        return controller.evaluate(req).getData();
    }
}
