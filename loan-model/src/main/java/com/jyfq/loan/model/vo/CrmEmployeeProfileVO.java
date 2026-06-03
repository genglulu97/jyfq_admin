package com.jyfq.loan.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class CrmEmployeeProfileVO implements Serializable {
    private Long id;
    private Long adminId;
    private String employeeName;
    private String phone;
    private Long teamId;
    private String teamName;
    private String crmRole;
    private Integer dailyClaimLimit;
    private Integer assignWeight;
    private Integer status;
    private String dataScope;
    private Integer currentCustomerCount;
    private Integer todayAssignedCount;
    private Integer todayFollowedCount;
    private Integer dealCount;
    private String remark;
    private LocalDateTime createdAt;
}
