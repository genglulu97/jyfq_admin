package com.jyfq.loan.model.vo;

import com.jyfq.loan.common.desensitize.Desensitize;
import com.jyfq.loan.common.desensitize.DesensitizeType;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Admin deduction record list item.
 */
@Data
public class DeductionRecordListVO implements Serializable {

    private Long id;
    private Long deductionRecordId;
    private String orderNo;

    @Desensitize(type = DesensitizeType.PHONE)
    private String phone;

    private Long channelId;
    private String channelCode;
    private String channelName;
    private Long instId;
    private String instCode;
    private String instName;
    private String merchantAlias;
    private Long productId;
    private String productName;
    private String deductInstitution;
    private String settlementMode;
    private BigDecimal settlementPrice;
    private BigDecimal amount;
    private BigDecimal deductAmount;
    private Integer financeStatus;
    private String financeStatusDesc;
    private Integer accountStatus;
    private String accountStatusDesc;
    private Integer orderStatus;
    private String orderStatusDesc;
    private String abnormalReason;
    private String rejectReason;
    private LocalDateTime applyTime;
    private LocalDateTime deductTime;
    private LocalDateTime createdAt;
    private String createBy;
    private LocalDateTime updatedAt;
    private String updateBy;
}
