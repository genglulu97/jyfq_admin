package com.jyfq.loan.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class CrmFollowRecordVO implements Serializable {
    private Long id;
    private Long customerId;
    private String customerName;
    private String mobile;
    private String mobileMasked;
    private Long followerAdminId;
    private String followerName;
    private Long teamId;
    private String followMethod;
    private String followResult;
    private String loanIntention;
    private Integer qualityStar;
    private String customerStatus;
    private String remark;
    private LocalDateTime followTime;
    private LocalDateTime nextFollowTime;
    private LocalDateTime createdAt;
}
