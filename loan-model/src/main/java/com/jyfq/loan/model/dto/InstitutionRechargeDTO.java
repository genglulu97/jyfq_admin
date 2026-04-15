package com.jyfq.loan.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Institution recharge request.
 */
@Data
public class InstitutionRechargeDTO implements Serializable {

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", inclusive = true, message = "金额必须大于0")
    private BigDecimal amount;

    private String remark;

    private String operatorName;
}
