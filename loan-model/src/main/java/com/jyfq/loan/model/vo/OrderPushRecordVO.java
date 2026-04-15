package com.jyfq.loan.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Admin push record view.
 */
@Data
public class OrderPushRecordVO implements Serializable {

    private Long id;
    private String orderNo;
    private String instCode;
    private String instName;
    private String productName;
    private String requestId;
    private String thirdOrderNo;
    private Integer pushStatus;
    private String pushStatusDesc;
    private String errorMsg;
    private Integer costMs;
    private LocalDateTime pushedAt;
    private LocalDateTime notifyAt;
    private LocalDateTime createdAt;
    private String createBy;
    private LocalDateTime updatedAt;
    private String updateBy;
}
