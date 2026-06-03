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
 * Product-level downstream pre-check detail under one collision record.
 */
@Data
@TableName("collision_precheck_record")
public class CollisionPrecheckRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long collisionId;
    private String collisionNo;
    private Long channelId;
    private String channelCode;
    private Long instId;
    private String instCode;
    private Long productId;
    private String productNameSnapshot;
    private String traceId;
    private String requestId;
    private String thirdOrderNo;
    private Integer precheckStatus;
    private String requestLog;
    private String responseLog;
    private BigDecimal downstreamPrice;
    private BigDecimal productCoefficientPrice;
    private BigDecimal upstreamChannelPrice;
    private String errorMsg;
    private Integer costMs;
    private LocalDateTime precheckedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
