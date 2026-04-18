package com.jyfq.loan.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Institution detail view.
 */
@Data
public class InstitutionDetailVO implements Serializable {

    private Long id;
    private String instCode;
    private String instName;
    private String merchantAlias;
    private String merchantType;
    private Integer status;
    private String statusDesc;
    private String adminPhone;
    private String adminName;
    private String adminRole;
    private Integer smsNotify;
    private Integer userStatus;
    private Integer crmAutoAssign;
    private Integer apiMerchant;
    private String specifiedChannel;
    private String excludedChannels;
    private String openCities;
    private String productName;
    private String productIcon;
    private Integer productAmount;
    private BigDecimal productRate;
    private Integer productPeriod;
    private String productProtocol;
    private BigDecimal accountBalance;
    private BigDecimal rechargeTotal;
    private String remark;
    private LocalDateTime createdAt;
    private String createBy;
    private LocalDateTime updatedAt;
    private String updateBy;
}
