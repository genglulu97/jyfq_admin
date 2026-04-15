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
 * Institution product and routing rules.
 */
@Data
@TableName("institution_product")
public class InstitutionProduct {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long instId;
    private String productName;
    private String productIcon;
    private Integer minAge;
    private Integer maxAge;
    private Integer minAmount;
    private Integer maxAmount;
    private BigDecimal rate;
    private Integer period;
    private String protocolUrl;
    private String cityNames;
    private String excludedCityCodes;
    private String excludedCityNames;
    private Integer cityMode;
    private String cityList;
    private String workingHours;
    private String specifiedChannels;
    private String excludedChannels;
    private String qualificationConfig;
    private Integer priority;
    private Integer weight;
    private Integer dailyQuota;
    private BigDecimal unitPrice;
    private BigDecimal priceRatio;
    private String remark;
    private String extJson;
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
}
