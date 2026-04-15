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
 * 灏忔椂缁熻蹇収锛堟姤琛ㄧ敤锛?
 */
@Data
@TableName("report_hourly")
public class ReportHourly {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 鏍煎紡: 2025-01-01 14 */
    private String statHour;

    /** 娓犻亾ID */
    private Long channelId;

    /** 鏈烘瀯ID */
    private Long instId;

    /** 杩涗欢鏁? */
    private Integer applyCnt;

    /** 鎺ㄥ崟鏁? */
    private Integer pushCnt;

    /** 鎺堜俊閫氳繃鏁? */
    private Integer approveCnt;

    /** 鏀炬鏁? */
    private Integer loanCnt;

    /** 鎵ｈ垂閲戦 */
    private BigDecimal deductAmount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
}
