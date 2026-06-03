package com.jyfq.loan.model.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Admin deduction record query parameters.
 */
@Data
public class DeductionRecordQueryDTO implements Serializable {

    private Long current = 1L;

    private Long size = 20L;

    private String orderNo;

    private String phone;

    private String channelCode;

    private String channelName;

    private String instCode;

    private String instName;

    /**
     * 1 deducted, 0 not deducted.
     */
    private Integer financeStatus;

    /**
     * Alias for financeStatus, kept for pages that name the column account status.
     */
    private Integer accountStatus;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deductStartTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deductEndTime;
}
