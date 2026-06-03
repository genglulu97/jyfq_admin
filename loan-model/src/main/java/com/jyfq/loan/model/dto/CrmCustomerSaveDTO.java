package com.jyfq.loan.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CrmCustomerSaveDTO implements Serializable {
    @NotBlank(message = "customerName is required")
    private String customerName;
    @NotBlank(message = "mobile is required")
    private String mobile;
    private String idCard;
    private String city;
    private Integer age;
    private String gender;
    private String occupation;
    private BigDecimal monthlyIncome;
    private Integer hasSocialSecurity;
    private Integer hasHousingFund;
    private Integer hasHouse;
    private Integer hasCar;
    private Integer sesameScore;
    private String creditCardStatus;
    private BigDecimal loanAmount;
    private String loanPurpose;
    private String expectedTerm;
    private String customerSource;
    private String channelCode;
    private Long crmInstId;
    private String crmInstCode;
    private String crmInstName;
    private Long sourceInstId;
    private String sourceInstCode;
    private String sourceInstName;
    private Long productId;
    private String productName;
    private String sourceOrderNo;
    private String sourceCollisionNo;
    private Long ownerAdminId;
    private Long teamId;
    private String customerStatus;
    private String loanIntention;
    private Integer qualityStar;
    private LocalDateTime nextFollowTime;
    private Integer isValid;
    private Integer wechatAdded;
    private Integer needRecall;
    private Integer isDeal;
    private Integer isRejected;
    private String wagePaymentType;
    private String socialSecurityStatus;
    private String housingFundStatus;
    private String houseStatus;
    private String carStatus;
    private String insuranceStatus;
    private String creditStatus;
    private Integer hasOverdue;
    private BigDecimal currentDebt;
    private Integer hasCreditCard;
    private Integer hasOnlineLoan;
    private BigDecimal acceptableRate;
    private Integer urgentMoney;
    private String remark;
}
