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
 * Application order.
 */
@Data
@TableName("apply_order")
public class ApplyOrder {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String orderNo;
    private Long channelId;
    private String channelCode;
    private Long instId;
    private Long productId;
    private Long pushId;
    private String traceId;
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
    private Integer orderStatus;
    private String rejectReason;
    private BigDecimal settlementPrice;
    private String followSalesman;
    private Integer salesmanRating;
    private String followRemark;
    private LocalDateTime allocationTime;
    private BigDecimal finalLoanAmount;
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
