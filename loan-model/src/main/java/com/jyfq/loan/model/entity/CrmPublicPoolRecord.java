package com.jyfq.loan.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("crm_public_pool_record")
public class CrmPublicPoolRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private String customerName;
    private Long previousOwnerAdminId;
    private String previousOwnerName;
    private Long operatorAdminId;
    private String operatorName;
    private String reason;
    private String actionType;
    private String remark;
    private LocalDateTime actionAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;
}
