package com.jyfq.loan.model.vo;

import com.jyfq.loan.common.desensitize.Desensitize;
import com.jyfq.loan.common.desensitize.DesensitizeType;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Admin order list item.
 */
@Data
public class OrderListVO implements Serializable {

    private Long id;
    private String orderNo;
    private String userName;

    @Desensitize(type = DesensitizeType.PHONE)
    private String phone;

    private String channelCode;
    private String channelName;
    private String cityName;
    private String merchantAlias;
    private Integer loanAmount;
    private String loanAmountRange;
    private String customerLevel;
    private Integer orderStatus;
    private String orderStatusDesc;
    private String followSalesman;
    private LocalDateTime createdAt;
    private String createBy;
    private LocalDateTime updatedAt;
    private String updateBy;
}
