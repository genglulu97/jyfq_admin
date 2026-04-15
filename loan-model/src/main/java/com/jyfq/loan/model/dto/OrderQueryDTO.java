package com.jyfq.loan.model.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Admin order query parameters.
 */
@Data
public class OrderQueryDTO implements Serializable {

    private Long current = 1L;

    private Long size = 20L;

    private String phone;

    private String userName;

    private String channelCode;

    private String merchantAlias;

    private String customerLevel;

    private String cityKeyword;

    private Integer orderStatus;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
