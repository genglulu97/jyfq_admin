package com.jyfq.loan.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Admin collision pre-check detail item.
 */
@Data
public class CollisionLogDetailVO implements Serializable {

    private Long id;
    private String collisionNo;
    private String requestId;
    private String thirdOrderNo;
    private Long instId;
    private String instCode;
    private String instName;
    private Long productId;
    private String productName;
    private Integer precheckStatus;
    private String precheckStatusDesc;
    private String result;
    private String resultDesc;
    private String rejectReason;
    private BigDecimal price;
    private BigDecimal downstreamPrice;
    private BigDecimal productCoefficientPrice;
    private BigDecimal upstreamChannelPrice;
    private Integer costMs;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String requestLog;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String responseLog;

    private LocalDateTime precheckedAt;
    private LocalDateTime createdAt;
}
