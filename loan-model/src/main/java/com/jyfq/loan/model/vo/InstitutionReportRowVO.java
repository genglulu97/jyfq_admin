package com.jyfq.loan.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Admin institution report row.
 */
@Data
public class InstitutionReportRowVO implements Serializable {

    private Long instId;

    private String instCode;

    private String instName;

    private String merchantAlias;

    private Long totalOrders = 0L;

    private Long successOrders = 0L;

    private String successRate = "0.00%";

    private BigDecimal totalAmount = BigDecimal.ZERO;

    private BigDecimal deductAmount = BigDecimal.ZERO;
}
