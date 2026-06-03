package com.jyfq.loan.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("crm_follow_record")
public class CrmFollowRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private String customerName;
    private String mobile;
    private Long followerAdminId;
    private String followerName;
    private Long teamId;
    private String followMethod;
    private String followResult;
    private String loanIntention;
    private Integer qualityStar;
    private String customerStatus;
    private String remark;
    private LocalDateTime followTime;
    private LocalDateTime nextFollowTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;
}
