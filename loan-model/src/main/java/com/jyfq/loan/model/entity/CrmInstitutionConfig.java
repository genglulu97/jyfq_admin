package com.jyfq.loan.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("crm_institution_config")
public class CrmInstitutionConfig {

    @TableId(type = IdType.AUTO)
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

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
}
