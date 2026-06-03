package com.jyfq.loan.model.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.io.Serializable;

@Data
public class CrmInstitutionConfigSaveDTO implements Serializable {
    private Long id;
    @JsonAlias({"inst_id", "institutionId", "institution_id", "merchantId", "merchant_id",
            "crmInstitutionId", "crm_institution_id", "crmMerchantId", "crm_merchant_id", "crmId", "crm_id",
            "bindingInstId", "binding_inst_id", "acrmInstId", "acrm_inst_id"})
    private Long instId;
    @JsonAlias({"platform_inst_id", "platformInstitutionId", "platform_institution_id",
            "platformMerchantId", "platform_merchant_id", "platformId", "platform_id",
            "merchantInstId", "merchant_inst_id", "targetInstId", "target_inst_id",
            "bindInstId", "bind_inst_id", "bcrmInstId", "bcrm_inst_id"})
    private Long platformInstId;
    @JsonAlias({"crm_inst_id", "crmInstitutionId", "crm_institution_id",
            "crmMerchantId", "crm_merchant_id", "crmId", "crm_id",
            "bindingInstId", "binding_inst_id", "acrmInstId", "acrm_inst_id"})
    private Long crmInstId;
    @JsonAlias({"autoCheck", "auto_check"})
    private Integer autoPush;
    private Integer autoAssign;
    private Long ownerAdminId;
    private Long teamId;
    private String customerSource;
    private Integer status;
    private String remark;
    private String crmOrgId;
    private String crmOrgName;
    private String crmOrgCode;
    private String crmAdminName;
    private String crmAdminPhone;
    private String crmAdminEmail;
    private String crmAdminRole;
    private String crmAdminAccount;
}
