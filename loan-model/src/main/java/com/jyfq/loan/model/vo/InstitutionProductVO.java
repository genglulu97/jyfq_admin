package com.jyfq.loan.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Institution product view.
 */
@Data
public class InstitutionProductVO implements Serializable {

    private Long id;
    private Long instId;
    private String productName;
    private String productIcon;
    private Integer maxAmount;
    private BigDecimal rate;
    private Integer period;
    private String protocolUrl;
    private Integer status;
    private String statusDesc;
    private LocalDateTime createdAt;
    private String createBy;
    private LocalDateTime updatedAt;
    private String updateBy;
}
