package com.jyfq.loan.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class CrmCustomerQueryDTO implements Serializable {
    private Long current;
    private Long size;
    private String customerName;
    private String mobile;
    private String city;
    private Integer age;
    private String gender;
    private String loanAmount;
    private String loanPurpose;
    private String customerSource;
    private String channelCode;
    private Long crmInstId;
    private String crmInstCode;
    private Long sourceInstId;
    private String sourceInstCode;
    private Long productId;
    private String productName;
    private String sourceOrderNo;
    private String sourceCollisionNo;
    private Long ownerAdminId;
    private Long teamId;
    private String customerStatus;
    private String loanIntention;
    private Integer qualityStar;
    private LocalDateTime lastFollowStart;
    private LocalDateTime lastFollowEnd;
    private LocalDateTime nextFollowStart;
    private LocalDateTime nextFollowEnd;
    private LocalDateTime createdStart;
    private LocalDateTime createdEnd;
    private Integer isAllocated;
    private Integer isCalled;
    private Integer isDuplicate;
    private Integer inPublicPool;
}
