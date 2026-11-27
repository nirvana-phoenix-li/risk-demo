package com.demo.risk.model;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

public class RiskEvaluateRequest {

    @NotBlank(message = "userId 不能为空")
    private String userId;

    @DecimalMin(value = "0", message = "amount 不能为负数")
    private BigDecimal amount;

    /** 业务场景：LOGIN / PAYMENT / WITHDRAW */
    private String scene;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }
}
