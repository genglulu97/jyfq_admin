package com.jyfq.loan.model.vo;

import com.jyfq.loan.common.desensitize.Desensitize;
import com.jyfq.loan.common.desensitize.DesensitizeType;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Admin order detail view.
 */
@Data
public class OrderDetailVO implements Serializable {

    private Long id;
    private String orderNo;
    private String channelCode;
    private String channelName;
    private String userName;

    @Desensitize(type = DesensitizeType.PHONE)
    private String phone;

    @Desensitize(type = DesensitizeType.ID_CARD)
    private String idCard;

    private String genderDesc;
    private String workCity;
    private Integer age;
    private String professionDesc;
    private String providentFundDesc;
    private String socialSecurityDesc;
    private String zhimaDesc;
    private String overdueDesc;
    private String houseDesc;
    private String vehicleDesc;
    private String vehicleStatus;
    private String vehicleValue;
    private String insuranceDesc;
    private String deviceIp;
    private Integer loanAmount;
    private String loanAmountRange;
    private String customerLevel;
    private BigDecimal settlementPrice;
    private String instCode;
    private String instName;
    private String productName;
    private String thirdOrderNo;
    private String merchantName;
    private String merchantAlias;
    private Integer pushStatus;
    private String pushStatusDesc;
    private String applyStatusDesc;
    private String orderStatusDesc;
    private String followSalesman;
    private Integer salesmanRating;
    private String followRemark;
    private LocalDateTime allocationTime;
    private BigDecimal finalLoanAmount;
    private LocalDateTime createdAt;
    private String createBy;
    private LocalDateTime updatedAt;
    private String updateBy;
}
