package com.jyfq.loan.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class CrmEmployeeProfileSaveDTO implements Serializable {
    @NotNull(message = "adminId is required")
    private Long adminId;
    private String employeeName;
    private String phone;
    private Long teamId;
    private String crmRole;
    private Integer dailyClaimLimit;
    private Integer assignWeight;
    private Integer status;
    private String dataScope;
    private String remark;
}
