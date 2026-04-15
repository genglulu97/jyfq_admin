package com.jyfq.loan.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Merchant recharge record view.
 */
@Data
public class InstitutionRechargeRecordVO implements Serializable {

    private Long id;
    private Long instId;
    private String merchantAlias;
    private String operatorName;
    private BigDecimal amount;
    private String remark;
    private LocalDateTime rechargeTime;
    private LocalDateTime createdAt;
    private String createBy;
    private LocalDateTime updatedAt;
    private String updateBy;
}
