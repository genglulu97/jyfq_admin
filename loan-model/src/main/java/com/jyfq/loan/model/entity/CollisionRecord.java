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
 * Pre-check / collision record.
 */
@Data
@TableName("collision_record")
public class CollisionRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String collisionNo;
    private Long channelId;
    private String channelCode;
    private Long instId;
    private String instCode;
    private Long productId;
    private String productNameSnapshot;
    private String traceId;
    private String phoneMd5;
    private String phoneEnc;
    private String idCardEnc;
    private String userName;
    private String userNameMd5;
    private Integer age;
    private String cityCode;
    private String workCity;
    /** Internal value: 1 male, 2 female, 0 unknown. */
    private Integer gender;
    /** Internal value: 1 employee, 2 freelancer, 3 business owner, 4 civil servant / state-owned enterprise. */
    private Integer profession;
    /** Internal value: actual representative Sesame score. */
    private Integer zhima;
    /** Internal value: 1 yes, 2 no. */
    private Integer house;
    /** Internal value: 1 yes, 2 no. */
    private Integer vehicle;
    private String vehicleStatus;
    private String vehicleValue;
    /** Internal value: 0 none, 1 <6 months, 2 6-12 months, 3 12+ months. */
    private Integer providentFund;
    /** Internal value: 0 none, 1 <6 months, 2 6-12 months, 3 12+ months. */
    private Integer socialSecurity;
    /** Internal value: 0 none, 1 <6 months, 2 6-12 months, 3 12+ months. */
    private Integer commercialInsurance;
    /** Internal value: 1 good credit, 2 currently overdue. */
    private Integer overdue;
    /** Internal value: actual amount, such as 30000/50000/100000/200000. */
    private Integer loanAmount;
    /** Internal value: actual months, such as 6/12/24/36. */
    private Integer loanTime;
    /** Downstream returned customer star level, reserved until institution query/update is integrated. */
    private String customerLevel;
    private String deviceIp;
    private Integer collisionStatus;
    private String rejectReason;
    private BigDecimal settlementPrice;
    private BigDecimal downstreamPrice;
    private BigDecimal productCoefficientPrice;
    private BigDecimal upstreamChannelPrice;
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
