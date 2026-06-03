package com.jyfq.loan.model.vo;

import com.jyfq.loan.common.desensitize.Desensitize;
import com.jyfq.loan.common.desensitize.DesensitizeType;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Admin collision log list item.
 */
@Data
public class CollisionLogListVO implements Serializable {

    private Long id;
    private String collisionNo;
    private String orderNo;
    private String requestId;
    private String thirdOrderNo;

    @Desensitize(type = DesensitizeType.PHONE)
    private String phone;

    @Desensitize(type = DesensitizeType.NAME)
    private String userName;

    private String channelCode;
    private String channelName;
    private Long instId;
    private String instCode;
    private String instName;
    private Long productId;
    private String productName;
    private String hitRule;
    private String matchRule;
    private Integer pushStatus;
    private String pushStatusDesc;
    private String result;
    private String resultDesc;
    private String rejectReason;
    private BigDecimal price;
    private BigDecimal downstreamPrice;
    private BigDecimal productCoefficientPrice;
    private BigDecimal upstreamChannelPrice;
    private Integer costMs;
    private LocalDateTime preCheckTime;
    private LocalDateTime createdAt;
}
