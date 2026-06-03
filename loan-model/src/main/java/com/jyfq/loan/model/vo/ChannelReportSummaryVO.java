package com.jyfq.loan.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Channel report summary metrics.
 */
@Data
public class ChannelReportSummaryVO implements Serializable {

    private Long totalOrders;

    private Long successOrders;

    private String successRate;

    private BigDecimal totalAmount;

    private BigDecimal averageAmount;
}
