package com.jyfq.loan.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Internal row for admin collision log query.
 */
@Data
public class CollisionLogRecordRow {

    private Long id;
    private String collisionNo;
    private String requestId;
    private String thirdOrderNo;
    private String phoneEnc;
    private String userNameEnc;
    private Long channelId;
    private String channelCode;
    private String channelName;
    private String channelAppKey;
    private Long instId;
    private String instCode;
    private String instName;
    private Long productId;
    private String productName;
    private Integer pushStatus;
    private String requestLog;
    private String responseLog;
    private String errorMsg;
    private Integer costMs;
    private LocalDateTime pushedAt;
    private LocalDateTime createdAt;
    private Integer collisionStatus;
    private String collisionRejectReason;
    private BigDecimal settlementPrice;
    private BigDecimal downstreamPrice;
    private BigDecimal productCoefficientPrice;
    private BigDecimal upstreamChannelPrice;
}
