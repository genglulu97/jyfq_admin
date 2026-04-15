package com.jyfq.loan.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Merchant recharge record.
 */
@Data
@TableName("institution_recharge_record")
public class InstitutionRechargeRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long instId;
    private String instCode;
    private String merchantAlias;
    private String operatorName;
    private BigDecimal amount;
    private String remark;
    private LocalDateTime rechargeTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
}
