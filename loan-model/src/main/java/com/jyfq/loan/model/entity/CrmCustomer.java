package com.jyfq.loan.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("crm_customer")
public class CrmCustomer {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String customerName;
    private String mobile;
    private String mobileMd5;
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
    private Long ownerAdminId;
    private String ownerName;
    private Long teamId;
    private String customerStatus;
    private String loanIntention;
    private Integer qualityStar;
    private Integer followCount;
    private LocalDateTime lastFollowTime;
    private LocalDateTime nextFollowTime;
    private String lastFollowRemark;
    private Integer isAllocated;
    private Integer isCalled;
    private Integer isDuplicate;
    private Integer isValid;
    private Integer wechatAdded;
    private Integer needRecall;
    private Integer isDeal;
    private Integer isRejected;
    private Integer isKeyCustomer;
    private Integer inPublicPool;
    private String publicPoolReason;
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

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
}
