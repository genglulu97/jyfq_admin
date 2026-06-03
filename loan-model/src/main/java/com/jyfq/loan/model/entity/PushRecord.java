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
 * Push record.
 */
@Data
@TableName("push_record")
public class PushRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;

    private String orderNo;

    private Long channelId;

    private Long instId;

    private String instCode;

    private Long productId;

    private String traceId;

    private String requestId;

    private String thirdOrderNo;

    private Integer pushStatus;

    private String requestLog;

    private String responseLog;

    private BigDecimal downstreamPrice;

    private BigDecimal productCoefficientPrice;

    private BigDecimal upstreamChannelPrice;

    private String errorMsg;

    private Integer costMs;

    private LocalDateTime pushedAt;

    private LocalDateTime notifyAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
}
