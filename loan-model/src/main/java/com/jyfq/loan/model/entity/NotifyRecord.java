package com.jyfq.loan.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Notify record for idempotency and troubleshooting.
 */
@Data
@TableName("notify_record")
public class NotifyRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String instCode;

    private String notifyNo;

    private String orderNo;

    private String status;

    private String rawBody;

    private String errorMsg;

    private Integer isProcessed;

    private LocalDateTime processedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
}
