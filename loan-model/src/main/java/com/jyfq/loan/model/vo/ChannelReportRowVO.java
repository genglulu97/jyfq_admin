package com.jyfq.loan.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Channel report table row.
 */
@Data
public class ChannelReportRowVO implements Serializable {

    private String statDate;

    private Long channelId;

    private String channelName;

    private String channelCode;

    private Long totalOrders;

    private Long successOrders;

    private String successRate;

    private BigDecimal totalAmount;

    private BigDecimal averageAmount;
}
