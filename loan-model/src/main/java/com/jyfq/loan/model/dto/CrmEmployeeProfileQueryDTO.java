package com.jyfq.loan.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CrmEmployeeProfileQueryDTO implements Serializable {
    private Long current;
    private Long size;
    private Long teamId;
    private String employeeName;
    private String crmRole;
    private Integer status;
}
