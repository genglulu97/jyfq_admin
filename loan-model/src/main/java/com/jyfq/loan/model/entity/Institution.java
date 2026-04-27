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
 * Institution / merchant config.
 */
@Data
@TableName("institution")
public class Institution {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String instCode;
    private String instName;
    private String merchantAlias;
    private String merchantType;
    private String businessCode;
    private String preCheckUrl;
    private String apiPushUrl;
    private String apiNotifyUrl;
    private String appKey;
    private String rsaPublicKey;
    private String encryptType;
    private String cipherMode;
    private String paddingMode;
    private String ivValue;
    private String notifyEncryptType;
    private String notifyCipherMode;
    private String notifyPaddingMode;
    private String notifyIvValue;
    private Integer timeoutMs;
    private Integer status;
    private String openCities;
    private String adminPhone;
    private String adminName;
    private String adminRole;
    private Integer smsNotify;
    private Integer userStatus;
    private Integer crmAutoAssign;
    private Integer apiMerchant;
    private String apiMethodName;
    private String specifiedChannel;
    private String excludedChannels;
    private BigDecimal accountBalance;
    private BigDecimal rechargeTotal;
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
