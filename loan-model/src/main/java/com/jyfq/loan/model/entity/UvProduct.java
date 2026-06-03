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
 * UV product configuration.
 */
@Data
@TableName("uv_product")
public class UvProduct {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String logo;
    private String status;
    private String position;
    private String loanType;
    private Integer minAmount;
    private Integer maxAmount;
    private String rate;
    private String term;
    private Integer weight;
    private BigDecimal price;
    private Integer uvThreshold;
    private String badge;
    private String isJoint;
    private String applyUrl;
    private String jointChannel;
    private String jointKey;
    private String jointCheckUrl;
    private String jointLoginUrl;
    private String jointRegAgreement;
    private LocalDateTime autoTimeStart;
    private LocalDateTime autoTimeEnd;
    private LocalDateTime autoOfflineTime;
    private String assocInst;
    private String specChannels;
    private Integer minAge;
    private Integer maxAge;
    private String blockProvinces;
    private String blockCities;
    private String targetRegions;
    private Integer applyCount;
    private String zhima;
    private String house;
    private String car;
    private String gongjijin;
    private String job;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
}
