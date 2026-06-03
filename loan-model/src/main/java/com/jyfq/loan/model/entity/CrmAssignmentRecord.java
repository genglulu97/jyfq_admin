package com.jyfq.loan.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("crm_assignment_record")
public class CrmAssignmentRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private String customerName;
    private Long fromAdminId;
    private String fromAdminName;
    private Long toAdminId;
    private String toAdminName;
    private Long assignerAdminId;
    private String assignerName;
    private String assignMode;
    private String remark;
    private LocalDateTime assignedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;
}
