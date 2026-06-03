package com.jyfq.loan.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Admin deduction record summary.
 */
@Data
public class DeductionRecordSummaryVO implements Serializable {

    private BigDecimal totalDeductAmount = BigDecimal.ZERO;
    private Long totalCount = 0L;
    private Long deductedCount = 0L;
    private Long undeductedCount = 0L;
    private Long successCount = 0L;
    private Long failedCount = 0L;
    private Long abnormalCount = 0L;
}
