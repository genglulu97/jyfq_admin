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
 * Independent institution customer record created after a successful apply.
 */
@Data
@TableName("institution_customer")
public class InstitutionCustomer {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String orderNo;
    private Long channelId;
    private String channelCode;
    private Long instId;
    private String instCode;
    private Long productId;
    private String productNameSnapshot;
    private String thirdOrderNo;
    private String phoneMd5;
    private String phoneEnc;
    private String idCardEnc;
    private String userName;
    private String userNameMd5;
    private Integer age;
    private String cityCode;
    private String workCity;
    private Integer gender;
    private Integer profession;
    private Integer zhima;
    private Integer house;
    private Integer vehicle;
    private String vehicleStatus;
    private String vehicleValue;
    private Integer providentFund;
    private Integer socialSecurity;
    private Integer commercialInsurance;
    private Integer overdue;
    private Integer loanAmount;
    private Integer loanTime;
    private String customerLevel;
    private String deviceIp;
    private BigDecimal settlementPrice;
    private Integer customerStatus;
    private String extJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
}
