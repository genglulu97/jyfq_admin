package com.jyfq.loan.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class CrmInstitutionConfigVO implements Serializable {
    private Long id;
    private Long instId;
    private String instCode;
    private String instName;
    private Long platformInstId;
    private String platformInstCode;
    private String platformInstName;
    private Long crmInstId;
    private String crmInstCode;
    private String crmInstName;
    private String crmOrgId;
    private String crmOrgName;
    private String crmOrgCode;
    private Integer autoPush;
    private Integer autoAssign;
    private Long ownerAdminId;
    private String ownerName;
    private String crmAdminName;
    private String crmAdminPhone;
    private String crmAdminEmail;
    private String crmAdminRole;
    private String crmAdminAccount;
    private Long teamId;
    private String customerSource;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
