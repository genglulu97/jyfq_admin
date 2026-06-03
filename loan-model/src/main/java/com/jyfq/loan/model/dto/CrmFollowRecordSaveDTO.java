package com.jyfq.loan.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class CrmFollowRecordSaveDTO implements Serializable {
    @NotNull(message = "customerId is required")
    private Long customerId;
    private String followMethod;
    private String followResult;
    private String loanIntention;
    private Integer qualityStar;
    private String customerStatus;
    private String remark;
    private LocalDateTime followTime;
    private LocalDateTime nextFollowTime;
    private Integer isValid;
    private Integer wechatAdded;
    private Integer needRecall;
    private Integer isDeal;
    private Integer isRejected;
}
