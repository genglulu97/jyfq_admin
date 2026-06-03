package com.jyfq.loan.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class CrmFollowRecordQueryDTO implements Serializable {
    private Long current;
    private Long size;
    private Long customerId;
    private String customerName;
    private String mobile;
    private Long followerAdminId;
    private String followMethod;
    private String followResult;
    private String loanIntention;
    private Integer qualityStar;
    private LocalDateTime followStart;
    private LocalDateTime followEnd;
    private LocalDateTime nextFollowStart;
    private LocalDateTime nextFollowEnd;
}
