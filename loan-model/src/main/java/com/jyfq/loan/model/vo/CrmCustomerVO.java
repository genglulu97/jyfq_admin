package com.jyfq.loan.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CrmCustomerVO implements Serializable {
    private Long id;
    private String customerName;
    private String mobile;
    private String mobileMasked;
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
    private String idCardMasked;
    private String city;
    private Integer age;
    private String gender;
    private BigDecimal loanAmount;
    private String loanPurpose;
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
    private Integer inPublicPool;
    private String publicPoolReason;
    private Integer isKeyCustomer;
    private LocalDateTime createdAt;
}
