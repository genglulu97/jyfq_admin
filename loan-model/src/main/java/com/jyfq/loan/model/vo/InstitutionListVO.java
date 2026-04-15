package com.jyfq.loan.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Institution list item.
 */
@Data
public class InstitutionListVO implements Serializable {

    private Long id;
    private String instCode;
    private String instName;
    private String merchantAlias;
    private String productName;
    private String merchantType;
    private String openCities;
    private Integer status;
    private String statusDesc;
    private BigDecimal accountBalance;
    private LocalDateTime createdAt;
    private String createBy;
    private LocalDateTime updatedAt;
    private String updateBy;
}
