package com.jyfq.loan.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Institution product routing list item.
 */
@Data
public class InstitutionProductListVO implements Serializable {

    private Long id;
    private Long instId;
    private String merchantAlias;
    private String statusDesc;
    private Integer status;
    private String cityNames;
    private BigDecimal unitPrice;
    private Integer cityQuota;
    private Integer weight;
    private LocalDateTime createdAt;
    private String createBy;
    private LocalDateTime updatedAt;
    private String updateBy;
    private String remark;
}
