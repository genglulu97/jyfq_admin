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
 * Upstream channel config.
 */
@Data
@TableName("channel")
public class Channel {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String channelId;
    private String channelCode;
    private String channelName;
    private String channelType;
    private String businessOwner;
    private Integer dailyQuota;
    private Integer normalRecommend;
    private Integer displayProductCount;
    private Integer actualPushCount;
    private String methodName;
    private String encryptType;
    private String cipherMode;
    private String paddingMode;
    private String ivValue;
    private String appKey;
    private String ipWhitelist;
    private String callbackUrl;
    private String settlementMode;
    private String extJson;
    private BigDecimal feeRate;
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
